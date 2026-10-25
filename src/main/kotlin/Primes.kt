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
