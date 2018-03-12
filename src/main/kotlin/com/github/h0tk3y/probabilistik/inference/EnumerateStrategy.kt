package com.github.h0tk3y.probabilistik.inference

import com.github.h0tk3y.probabilistik.ContinuationBranching
import com.github.h0tk3y.probabilistik.ProbabilisticContext
import com.github.h0tk3y.probabilistik.ProbabilisticProgram
import com.github.h0tk3y.probabilistik.distribution.Distribution
import com.github.h0tk3y.probabilistik.distribution.FiniteDistribution
import com.github.h0tk3y.probabilistik.normalize

fun <T> ProbabilisticProgram<T>.enumerate(): Map<T, Double> = infer(EnumerateStrategy())

open class EnumerateStrategy<T>(val limitExploredPaths: Int? = null) : InferenceStrategy<T, Map<T, Double>> {
    init {
        require(limitExploredPaths?.let { it > 0 } ?: true) {
            "The limitation on number of explored paths should be positive"
        }
    }

    private var exploredPaths = 0

    override val result: Map<T, Double> get() = outcomeWeights.normalize()

    override fun reachedEndOfComputation(returnValue: T, probabilisticContext: ProbabilisticContext<T, Map<T, Double>>) {
        ++exploredPaths
        val resultProbability = Math.exp(probabilisticContext.currentLogProbability)
        outcomeWeights.merge(returnValue, resultProbability, Double::plus)
    }

    override fun factoring(
        branching: ContinuationBranching<Unit>,
        factoringFunction: () -> Double,
        probabilisticContext: ProbabilisticContext<T, Map<T, Double>>
    ) {
        val factoringLogProbability = factoringFunction()
        if (probabilisticContext.currentLogProbability + factoringLogProbability == Double.NEGATIVE_INFINITY) {
            return
        }
        super.factoring(branching, { factoringLogProbability }, probabilisticContext)
    }

    override fun <R> sampleBranching(
        branching: ContinuationBranching<R>,
        distributionToSample: Distribution<R>,
        probabilisticContext: ProbabilisticContext<T, Map<T, Double>>
    ) {
        for (r in (distributionToSample as FiniteDistribution).support) {
            if (limitExploredPaths != null && exploredPaths >= limitExploredPaths) {
                break
            }

            branching.runBranch(r, beforeResume = {
                probabilisticContext.applyFactoring(distributionToSample.logLikelihood(r))
            })
        }
    }

    private val outcomeWeights = mutableMapOf<T, Double>()
}