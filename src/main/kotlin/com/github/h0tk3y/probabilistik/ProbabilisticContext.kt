package com.github.h0tk3y.probabilistik

import com.github.h0tk3y.probabilistik.distribution.Distribution
import com.github.h0tk3y.probabilistik.inference.InferenceStrategy
import java.util.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

@RestrictsSuspension
open class ProbabilisticContext<T, R>(
    private val strategy: InferenceStrategy<T, R>
) : Continuation<T> {

    override fun resume(value: T) = strategy.reachedEndOfComputation(value, this)

    override fun resumeWithException(exception: Throwable) = throw exception

    override val context: CoroutineContext get() = EmptyCoroutineContext

    internal var logFactorsStack = Stack<Double>().apply { push(0.0) }

    var currentLogProbability: Double = 0.0
        internal set

    private fun enterComputation() {
        logFactorsStack.push(currentLogProbability)
    }

    private fun exitComputation() {
        val previousLogProbability = logFactorsStack.pop()
        currentLogProbability = previousLogProbability
    }

    private fun <R> continueAndRollback(continuation: Continuation<R>, value: R) {
        continuation.resume(value)
    }

    suspend fun factor(factoringFunction: () -> Double): Unit =
        suspendCoroutineOrReturn { c ->
            strategy.factoring(ContextContinuationBranching(c), factoringFunction, this)
            COROUTINE_SUSPENDED
        }

    suspend fun observe(evidence: () -> Boolean) =
        factor { if (evidence()) 0.0 else Double.NEGATIVE_INFINITY }

    fun applyFactoring(logProbability: Double) {
        currentLogProbability += logProbability
    }

    suspend fun <R> sample(distribution: Distribution<R>): R =
        suspendCoroutineOrReturn { c ->
            val branching = ContextContinuationBranching(c)
            strategy.sampleBranching(branching, distribution, this)
            COROUTINE_SUSPENDED
        }

    suspend operator fun <E> ProbabilisticProgram<E>.invoke(): E = computation()

    private inner class ContextContinuationBranching<R>(val continuation: Continuation<R>) : ContinuationBranching<R> {
        private val stateHere = continuation.stateStack

        override fun runBranch(withValue: R, beforeResume: () -> Unit, afterResume: () -> Unit) {
            continuation.stateStack = stateHere
            enterComputation()
            beforeResume()
            continueAndRollback(continuation, withValue)
            afterResume()
            exitComputation()
        }
    }
}

interface ContinuationBranching<R> {
    fun runBranch(
        withValue: R,
        beforeResume: () -> Unit = { },
        afterResume: () -> Unit = { }
    )
}