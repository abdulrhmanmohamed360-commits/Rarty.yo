package com.cuptracker.core

import kotlin.math.abs

/** Tiny dense matrix, just enough for a 6-state Kalman filter. */
class Matrix(val rows: Int, val cols: Int, val d: DoubleArray = DoubleArray(rows * cols)) {
    operator fun get(r: Int, c: Int) = d[r * cols + c]
    operator fun set(r: Int, c: Int, v: Double) { d[r * cols + c] = v }

    operator fun times(o: Matrix): Matrix {
        require(cols == o.rows) { "dimension mismatch" }
        val out = Matrix(rows, o.cols)
        for (i in 0 until rows) for (k in 0 until cols) {
            val a = this[i, k]
            if (a == 0.0) continue
            for (j in 0 until o.cols) out.d[i * o.cols + j] += a * o[k, j]
        }
        return out
    }
    operator fun plus(o: Matrix) = Matrix(rows, cols, DoubleArray(d.size) { d[it] + o.d[it] })
    operator fun minus(o: Matrix) = Matrix(rows, cols, DoubleArray(d.size) { d[it] - o.d[it] })
    fun t(): Matrix { val o = Matrix(cols, rows); for (i in 0 until rows) for (j in 0 until cols) o[j, i] = this[i, j]; return o }
    fun copy() = Matrix(rows, cols, d.copyOf())

    fun inv(): Matrix {
        require(rows == cols) { "square only" }
        val n = rows
        val a = Matrix(n, 2 * n)
        for (i in 0 until n) { for (j in 0 until n) a[i, j] = this[i, j]; a[i, n + i] = 1.0 }
        for (c in 0 until n) {
            var p = c
            for (r in c + 1 until n) if (abs(a[r, c]) > abs(a[p, c])) p = r
            if (p != c) for (j in 0 until 2 * n) { val tmp = a[c, j]; a[c, j] = a[p, j]; a[p, j] = tmp }
            var piv = a[c, c]
            if (abs(piv) < 1e-12) piv = 1e-12
            for (j in 0 until 2 * n) a[c, j] /= piv
            for (r in 0 until n) if (r != c) {
                val f = a[r, c]
                if (f != 0.0) for (j in 0 until 2 * n) a[r, j] -= f * a[c, j]
            }
        }
        val out = Matrix(n, n)
        for (i in 0 until n) for (j in 0 until n) out[i, j] = a[i, n + j]
        return out
    }

    companion object {
        fun identity(n: Int) = Matrix(n, n).also { for (i in 0 until n) it[i, i] = 1.0 }
        fun diag(vararg v: Double) = Matrix(v.size, v.size).also { for (i in v.indices) it[i, i] = v[i] }
    }
}
