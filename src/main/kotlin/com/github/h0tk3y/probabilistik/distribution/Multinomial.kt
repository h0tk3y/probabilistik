package com.github.h0tk3y.probabilistik.distribution

import com.github.h0tk3y.probabilistik.normalize

class Multinomial<T>(val probabilities: Map<T, Double>) : FiniteDistribution<T> {
    init {
        require(probabilities.isNotEmpty()) { "At least one value is required." }
        require(probabilities.values.all { it >= 0.0 }) { "All probabilities should be non-negative." }
    }

    private val normalizedProbabilities = probabilities.normalize()

    private val scores = probabilities.normalize().mapValues { Math.log(it.value) }

    private val valuesList = probabilities.keys.toList()
    private val accumulatedP = run {
        var p = 0.0
        normalizedProbabilities.values.map { p += it; p }
    }

    override fun takeSample(): T {
        val d = random.nextDouble()
        val index = accumulatedP.binarySearch(d)
            .let { if (it > 0) it else -it - 1 }
            .coerceAtMost(accumulatedP.lastIndex)
        return valuesList[index]
    }

    constructor(keys: List<T>, probabilityFunction: (T) -> Double) :
        this(keys.associate { it to probabilityFunction(it) }.normalize())

    override fun logLikelihood(value: T): Double = scores[value] ?: 0.0

    override val support: Iterable<T>
        get() = normalizedProbabilities.keys
}

fun randomInteger(range: IntRange) =
    Multinomial(range.associate { it to 1.0 / (range.endInclusive - range.start + 1) })
