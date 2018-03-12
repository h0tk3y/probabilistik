package com.github.h0tk3y.probabilistik.distribution

import kotlin.math.ln

class Uniform(val range: ClosedFloatingPointRange<Double>) : Distribution<Double> {
    private val rangeLength = range.endInclusive - range.start

    override fun takeSample(): Double =
        random.nextDouble() * rangeLength + range.start

    override fun logLikelihood(value: Double): Double = -ln(rangeLength)
}