import java.io.File

private const val WORKER_COMMAND = "worker"

fun runSequential(size: Int, maxValue: Int): Long =
    processRange(generateValues(size, maxValue), 0, size)

/** Запускает processes отдельных JVM-процессов; каждый обрабатывает свой непрерывный диапазон списка. */
fun runParallel(size: Int, maxValue: Int, processes: Int): Long {
    val workers = splitRanges(size, processes).map { startWorker(size, maxValue, it) }
    return workers.sumOf(::collectResult)
}

private fun startWorker(size: Int, maxValue: Int, range: IntRange): Process {
    val java = File(System.getProperty("java.home"), "bin/java").path
    val classpath = System.getProperty("java.class.path")
    val args = listOf(WORKER_COMMAND, size, maxValue, range.first, range.last + 1).map(Any::toString)
    return ProcessBuilder(listOf(java, "-cp", classpath, "MainKt") + args)
        .redirectErrorStream(true)
        .start()
}

private fun collectResult(worker: Process): Long {
    val output = worker.inputStream.bufferedReader().readText().trim()
    check(worker.waitFor() == 0) { "Воркер завершился с ошибкой: $output" }
    return output.toLong()
}

/** Точка входа процесса-воркера: args = size maxValue from to; результат печатается в stdout. */
fun runWorker(args: List<String>) {
    val (size, maxValue, from, to) = args.map { it.toInt() }
    println(processRange(generateValues(size, maxValue), from, to))
}

fun isWorkerCommand(command: String?): Boolean = command == WORKER_COMMAND
