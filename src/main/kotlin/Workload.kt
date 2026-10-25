import java.util.Random

const val DEFAULT_MAX_VALUE = 500_000
const val SEED = 42L

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

/** Разбивает [0; size) на не более parts непрерывных диапазонов одинаковой длины (последний может быть короче). */
fun splitRanges(size: Int, parts: Int): List<IntRange> {
    val chunk = (size + parts - 1) / parts
    return (0 until parts)
        .map { it * chunk until minOf(size, (it + 1) * chunk) }
        .filter { !it.isEmpty() }
}
