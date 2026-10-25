import java.io.File
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit

const val DEFAULT_MAX_VALUE = 500_000
const val SEED = 42L
const val REPEATS = 3
val SIZES = listOf(1_000, 5_000, 20_000)
val PROCESS_COUNTS = listOf(1, 2, 4, 8)

/** Число простых чисел, не превосходящих n (решето Эратосфена). */
fun countPrimes(n: Int): Int {
    if (n < 2) return 0
    val composite = BooleanArray(n + 1)
    var i = 2
    while (i.toLong() * i <= n) {
        if (!composite[i]) {
            var j = i * i
            while (j <= n) {
                composite[j] = true
                j += i
            }
        }
        i++
    }
    var count = 0
    for (k in 2..n) if (!composite[k]) count++
    return count
}

/** Список из size значений в диапазоне [maxValue / 2; maxValue]; одинаков при каждом запуске. */
fun generateValues(size: Int, maxValue: Int): IntArray {
    val random = Random(SEED)
    val low = maxValue / 2
    return IntArray(size) { low + random.nextInt(maxValue - low + 1) }
}

/** Сумма countPrimes по элементам values[from, to): контрольная сумма и полезная нагрузка. */
fun processRange(values: IntArray, from: Int, to: Int): Long {
    var sum = 0L
    for (i in from until to) sum += countPrimes(values[i])
    return sum
}

fun runSequential(size: Int, maxValue: Int): Long =
    processRange(generateValues(size, maxValue), 0, size)

/** Запускает processes отдельных JVM-процессов; каждый обрабатывает свой непрерывный диапазон списка. */
fun runParallel(size: Int, maxValue: Int, processes: Int): Long {
    val java = File(System.getProperty("java.home"), "bin/java").path
    val classpath = System.getProperty("java.class.path")
    val chunk = (size + processes - 1) / processes
    val workers = (0 until processes).mapNotNull { index ->
        val from = index * chunk
        val to = minOf(size, from + chunk)
        if (from >= to) return@mapNotNull null
        ProcessBuilder(java, "-cp", classpath, "MainKt", "worker", size.toString(), maxValue.toString(), from.toString(), to.toString())
            .redirectErrorStream(true)
            .start()
    }
    return workers.sumOf { worker ->
        val output = worker.inputStream.bufferedReader().readText().trim()
        check(worker.waitFor() == 0) { "Воркер завершился с ошибкой: $output" }
        output.toLong()
    }
}

fun <T> timed(block: () -> T): Pair<T, Double> {
    val start = System.nanoTime()
    val result = block()
    return result to (System.nanoTime() - start) / 1e9
}

fun fmt(digits: Int, value: Double): String = String.format(Locale.US, "%.${digits}f", value)

fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

fun bench(maxValue: Int, output: File) {
    output.parentFile?.mkdirs()
    val lines = mutableListOf("mode,n_values,processes,time_median_s,time_runs_s,speedup_vs_1_process,checksum")
    for (size in SIZES) {
        val (expected, _) = timed { runSequential(size, maxValue) }
        val sequentialRuns = List(REPEATS) { timed { runSequential(size, maxValue) }.second }
        val sequentialMedian = median(sequentialRuns)
        println("N=$size sequential: ${fmt(3, sequentialMedian)} s")
        lines += "sequential,$size,1,${fmt(4, sequentialMedian)},${sequentialRuns.joinToString(";") { fmt(4, it) }},,$expected"

        var oneProcess = 0.0
        for (processes in PROCESS_COUNTS) {
            val runs = List(REPEATS) {
                val (checksum, seconds) = timed { runParallel(size, maxValue, processes) }
                check(checksum == expected) { "Контрольная сумма не совпала: $checksum != $expected (N=$size, p=$processes)" }
                seconds
            }
            val runsMedian = median(runs)
            if (processes == 1) oneProcess = runsMedian
            val speedup = oneProcess / runsMedian
            println("N=$size p=$processes: ${fmt(3, runsMedian)} s, speedup ${fmt(2, speedup)}")
            lines += "parallel,$size,$processes,${fmt(4, runsMedian)},${runs.joinToString(";") { fmt(4, it) }},${fmt(3, speedup)},$expected"
        }
    }
    output.writeText(lines.joinToString("\n") + "\n")
    println("Результаты записаны в ${output.path}")
}

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "worker" -> {
            val (size, maxValue, from, to) = args.drop(1).map { it.toInt() }
            println(processRange(generateValues(size, maxValue), from, to))
        }
        "bench" -> bench(args.getOrNull(1)?.toInt() ?: DEFAULT_MAX_VALUE, File(args.getOrNull(2) ?: "results/results.csv"))
        else -> println("Использование: bench [maxValue] [csv] | worker <size> <maxValue> <from> <to>")
    }
}
