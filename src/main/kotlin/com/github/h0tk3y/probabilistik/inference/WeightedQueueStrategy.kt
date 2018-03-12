package com.github.h0tk3y.probabilistik.inference

import com.github.h0tk3y.probabilistik.ContinuationBranching
import com.github.h0tk3y.probabilistik.ProbabilisticContext
import com.github.h0tk3y.probabilistik.distribution.Distribution
import com.github.h0tk3y.probabilistik.distribution.FiniteDistribution
import java.util.*

class WeightedQueueStrategy<T>(limitExploredPaths: Int? = null) : EnumerateStrategy<T>(limitExploredPaths) {
    override fun <R> sampleBranching(
        branching: ContinuationBranching<R>,
        distributionToSample: Distribution<R>,
        probabilisticContext: ProbabilisticContext<T, Map<T, Double>>
    ) {
        val newBranches = (distributionToSample as FiniteDistribution).support.map { value ->
            val p = distributionToSample.logLikelihood(value)
            BranchWithProbability(
                probabilisticContext,
                probabilisticContext.currentLogProbability,
                p, value, branching)
        }
        prioritizedBranches.addAll(newBranches)
        while (prioritizedBranches.isNotEmpty()) {
            val branch = prioritizedBranches.remove()
            resumeSingleBranch(branch)
        }
    }

    private class BranchWithProbability<R>(
        val context: ProbabilisticContext<*, *>,
        val currentLogProbability: Double,
        val enterLogProbability: Double,
        val value: R,
        val branching: ContinuationBranching<R>
    ) {
        val logFactorsStack: List<Double> =
            context.logFactorsStack.toList()

        val posterior: Double get() = currentLogProbability + enterLogProbability
    }

    private fun <T> resumeSingleBranch(branch: BranchWithProbability<T>) {
        branch.context.logFactorsStack = Stack<Double>().apply { addAll(branch.logFactorsStack) }
        branch.context.currentLogProbability = branch.currentLogProbability
        branch.branching.runBranch(
            branch.value,
            beforeResume = { branch.context.applyFactoring(branch.enterLogProbability) },
            afterResume = {
                branch.context.logFactorsStack = Stack<Double>().apply {
                    addAll(branch.logFactorsStack); add(branch.posterior)
                }
            })
    }

    private val prioritizedBranches =
        PriorityQueue<BranchWithProbability<*>>(compareBy { -it.posterior })
}