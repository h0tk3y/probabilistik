package com.github.h0tk3y.probabilistik.distribution

import java.util.*

interface Distribution<T> {
    /** Returns a random sample of this [Distribution]. */
    fun takeSample(): T

    /** Returns the log-likelihood of the [value] for this [Distribution]. */
    fun logLikelihood(value: T): Double
}

interface FiniteDistribution<T> : Distribution<T> {
    /** Returns the [Iterable] of all possible values of this [Distribution]. */
    val support: Iterable<T>
}

internal val random =
    System.getProperty("probabilistik.seed")?.toLongOrNull()?.let { Random(it) }
    ?: Random()
