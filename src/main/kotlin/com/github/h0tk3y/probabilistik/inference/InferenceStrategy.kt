package com.github.h0tk3y.probabilistik.inference

import com.github.h0tk3y.probabilistik.ContinuationBranching
import com.github.h0tk3y.probabilistik.ProbabilisticContext
import com.github.h0tk3y.probabilistik.ProbabilisticProgram
import com.github.h0tk3y.probabilistik.distribution.Distribution
import kotlin.coroutines.experimental.startCoroutine

interface InferenceStrategy<T, Q> {
    fun reachedEndOfComputation(
        returnValue: T,
        probabilisticContext: ProbabilisticContext<T, Q>
    )

    val result: Q

    fun factoring(
        branching: ContinuationBranching<Unit>,
        factoringFunction: () -> Double,
        probabilisticContext: ProbabilisticContext<T, Q>
    ) {
        val logProbability = factoringFunction()
        probabilisticContext.applyFactoring(logProbability)
        branching.runBranch(Unit)
    }

    fun <R> sampleBranching(
        branching: ContinuationBranching<R>,
        distributionToSample: Distribution<R>,
        probabilisticContext: ProbabilisticContext<T, Q>
    )
}

fun <T, Q, S : InferenceStrategy<T, Q>> ProbabilisticProgram<T>.infer(strategy: S): Q =
    strategy.run strategy@{
        val probabilisticContext: ProbabilisticContext<T, Q> = ProbabilisticContext(this@strategy)
        this@infer.computation.startCoroutine(probabilisticContext, probabilisticContext)
        this@strategy.result
    }