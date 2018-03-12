package com.github.h0tk3y.probabilistik.distribution

import kotlin.math.PI
import kotlin.math.ln

class Gaussian(
    val mean: Double,
    val variance: Double
) : Distribution<Double> {

    init {
        require(variance >= 0) { "The 'variance' parameter should be non-negative" }
    }

    private val stdDeviation = Math.sqrt(variance)

    override fun takeSample(): Double =
        random.nextGaussian() * stdDeviation + mean

    private val scoreConst = -0.5 * (ln(2 * PI) + ln(variance))

    override fun logLikelihood(value: Double): Double =
        scoreConst - 0.5 / variance * (value - mean).let { it * it }
}