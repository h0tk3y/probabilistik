package com.github.h0tk3y.probabilistik.distribution

class Bernoulli(val p: Double) : FiniteDistribution<Boolean> {
    init {
        require(p in 0.0..1.0) { "The probability 'p' should be inside 0..1" }
    }

    override fun takeSample(): Boolean = random.nextDouble() < p

    private val logP = Math.log(p)
    private val log1p = Math.log(1.0 - p)

    override fun logLikelihood(value: Boolean): Double = if (value) logP else log1p

    override val support: Set<Boolean> = setOf(true, false)
}