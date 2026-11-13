package com.cuptracker.core

/** Optimal rectangular assignment (Kuhn-Munkres, O(n^2 m)). Returns row -> column or -1. */
object HungarianMatcher {
    const val BIG = 1e6

    fun solve(cost: Array<DoubleArray>): IntArray {
        val n = cost.size
        if (n == 0) return IntArray(0)
        val m = cost[0].size
        if (m == 0) return IntArray(n) { -1 }
        if (n > m) {
            val t = Array(m) { r -> DoubleArray(n) { c -> cost[c][r] } }
            val a = solve(t)
            val res = IntArray(n) { -1 }
            for (r in 0 until m) if (a[r] >= 0) res[a[r]] = r
            return res
        }
        val u = DoubleArray(n + 1); val v = DoubleArray(m + 1)
        val p = IntArray(m + 1); val way = IntArray(m + 1)
        for (i in 1..n) {
            p[0] = i
            var j0 = 0
            val minv = DoubleArray(m + 1) { Double.MAX_VALUE }
            val used = BooleanArray(m + 1)
            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = Double.MAX_VALUE
                var j1 = 0
                for (j in 1..m) if (!used[j]) {
                    val cur = cost[i0 - 1][j - 1] - u[i0] - v[j]
                    if (cur < minv[j]) { minv[j] = cur; way[j] = j0 }
                    if (minv[j] < delta) { delta = minv[j]; j1 = j }
                }
                for (j in 0..m) if (used[j]) { u[p[j]] += delta; v[j] -= delta } else minv[j] -= delta
                j0 = j1
            } while (p[j0] != 0)
            do { val j1 = way[j0]; p[j0] = p[j1]; j0 = j1 } while (j0 != 0)
        }
        val res = IntArray(n) { -1 }
        for (j in 1..m) if (p[j] != 0) res[p[j] - 1] = j - 1
        return res
    }
}
