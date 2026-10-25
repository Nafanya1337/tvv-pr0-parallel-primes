import java.io.File

private const val DEFAULT_CSV = "results/results.csv"

fun main(args: Array<String>) {
    when {
        isWorkerCommand(args.firstOrNull()) -> runWorker(args.drop(1))
        args.firstOrNull() == "bench" ->
            bench(args.getOrNull(1)?.toInt() ?: DEFAULT_MAX_VALUE, File(args.getOrNull(2) ?: DEFAULT_CSV))
        else -> println("Использование: bench [maxValue] [csv] | worker <size> <maxValue> <from> <to>")
    }
}
