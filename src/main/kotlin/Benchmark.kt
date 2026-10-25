import java.io.File
import java.util.Locale

const val REPEATS = 3
val SIZES = listOf(1_000, 5_000, 20_000)
val PROCESS_COUNTS = listOf(1, 2, 4, 8)

private const val CSV_HEADER = "mode,n_values,processes,time_median_s,time_runs_s,speedup_vs_1_process,checksum"

/** Строка результатов; speedup = null для последовательного режима. */
private class Row(
    val mode: String,
    val size: Int,
    val processes: Int,
    val runs: List<Double>,
    val speedup: Double?,
    val checksum: Long,
) {
    val median: Double = median(runs)

    fun toCsv(): String =
        listOf(
            mode, size, processes, fmt(4, median),
            runs.joinToString(";") { fmt(4, it) },
            speedup?.let { fmt(3, it) } ?: "",
            checksum,
        ).joinToString(",")
}

private fun <T> timed(block: () -> T): Pair<T, Double> {
    val start = System.nanoTime()
    val result = block()
    return result to (System.nanoTime() - start) / 1e9
}

private fun fmt(digits: Int, value: Double): String = String.format(Locale.US, "%.${digits}f", value)

private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

/** REPEATS замеров; результат каждого запуска проверяется на равенство ожидаемой контрольной сумме. */
private fun measure(expected: Long, label: String, run: () -> Long): List<Double> =
    List(REPEATS) {
        val (checksum, seconds) = timed(run)
        check(checksum == expected) { "Контрольная сумма не совпала: $checksum != $expected ($label)" }
        seconds
    }

private fun benchSize(size: Int, maxValue: Int): List<Row> {
    // Первый прогон прогревает JIT и задаёт эталонную контрольную сумму.
    val expected = runSequential(size, maxValue)
    val sequentialRuns = measure(expected, "N=$size, sequential") { runSequential(size, maxValue) }
    val sequential = Row("sequential", size, 1, sequentialRuns, null, expected)
    println("N=$size sequential: ${fmt(3, sequential.median)} s")

    val parallel = mutableListOf<Row>()
    for (processes in PROCESS_COUNTS) {
        val runs = measure(expected, "N=$size, p=$processes") { runParallel(size, maxValue, processes) }
        val baseline = parallel.firstOrNull()?.median ?: median(runs)
        val row = Row("parallel", size, processes, runs, baseline / median(runs), expected)
        println("N=$size p=$processes: ${fmt(3, row.median)} s, speedup ${fmt(2, row.speedup!!)}")
        parallel += row
    }
    return listOf(sequential) + parallel
}

fun bench(maxValue: Int, output: File) {
    output.parentFile?.mkdirs()
    val rows = SIZES.flatMap { benchSize(it, maxValue) }
    output.writeText((listOf(CSV_HEADER) + rows.map(Row::toCsv)).joinToString("\n") + "\n")
    println("Результаты записаны в ${output.path}")
}
