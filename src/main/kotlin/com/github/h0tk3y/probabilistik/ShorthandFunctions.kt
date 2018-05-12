package com.github.h0tk3y.probabilistik

import com.github.h0tk3y.probabilistik.distribution.Bernoulli
import com.github.h0tk3y.probabilistik.distribution.randomInteger

fun <T> probabilistic(computation: suspend ProbabilisticContext<*, *>.() -> T) = ProbabilisticProgram(computation)

suspend fun <T> ProbabilisticContext<*, *>.roll(range: IntRange) = sample(randomInteger(range))

suspend fun ProbabilisticContext<*, *>.flip(pTrue: Double) = sample(Bernoulli(pTrue))