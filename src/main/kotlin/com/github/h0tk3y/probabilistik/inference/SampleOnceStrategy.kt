package com.github.h0tk3y.probabilistik.inference

import com.github.h0tk3y.probabilistik.ContinuationBranching
import com.github.h0tk3y.probabilistik.ProbabilisticContext
import com.github.h0tk3y.probabilistik.distribution.Distribution

sealed class SampleOnceResult<out T>
data class ValueWithProbability<out T>(val value: T, val likelihood: Double) : SampleOnceResult<T>()
object Impossible : SampleOnceResult<Nothing>()

class SampleOnceStrategy<T> : InferenceStrategy<T, SampleOnceResult<T>> {
    override fun reachedEndOfComputation(
        returnValue: T,
        probabilisticContext: ProbabilisticContext<T, SampleOnceResult<T>>
    ) {
        result = ValueWithProbability(returnValue, Math.exp(probabilisticContext.currentLogProbability))
    }

    override fun factoring(
        branching: ContinuationBranching<Unit>,
        factoringFunction: () -> Double,
        probabilisticContext: ProbabilisticContext<T, SampleOnceResult<T>>
    ) {
        val factoringLogProbability = factoringFunction()
        val resultingLogProbability = probabilisticContext.currentLogProbability + factoringLogProbability
        if (resultingLogProbability == Double.NEGATIVE_INFINITY) {
            return
        }
        super.factoring(branching, { factoringLogProbability }, probabilisticContext)
    }

    override fun <R> sampleBranching(branching: ContinuationBranching<R>, distributionToSample: Distribution<R>, probabilisticContext: ProbabilisticContext<T, SampleOnceResult<T>>) {
        if (probabilisticContext.currentLogProbability == Double.NEGATIVE_INFINITY) {
            return
        }
        val singleValue = distributionToSample.takeSample()
        probabilisticContext.applyFactoring(distributionToSample.logLikelihood(singleValue))
        branching.runBranch(singleValue)
    }

    override var result: SampleOnceResult<T> = Impossible
        private set
}