package com.github.h0tk3y.probabilistik

fun <T> Map<T, Double>.normalize(): Map<T, Double> {
    val sumValues = values.sum()
    return if (sumValues == 0.0) this else mapValues{ (_, v) -> v / sumValues }
}

@JvmName("normalizeInts")
fun <T> Map<T, Int>.normalize(): Map<T, Double> = mapValues { it.value.toDouble() }.normalize()