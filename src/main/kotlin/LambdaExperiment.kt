import java.util.*

fun main(args: Array<String>) {
    val probabilities = simplyMarginalize(myProgram)
    println(probabilities)
}

val myProgram: ProbabilisticProgram<Boolean> = {
    val d = Bernoulli(0.5)
    sample(from = d) { d1 ->
        sample(from = d) { d2 ->
            observe { d1 or d2 }
            endWith(d1)
        }
    }
}

typealias ProbabilisticProgram<T> = ProbabilisticContext<T>.() -> ProbabilisticComputation<T>

sealed class ProbabilisticComputation<T>

class ProbabilisticContext<T>(
    private val strategy: InferenceStrategy<T>
) {
    private val logFactorsStack = Stack<Double>().apply { push(0.0) }
    var currentLogProbability: Double = 0.0

    internal fun handleComputation(computation: ProbabilisticComputation<T>) {
        when (computation) {
            is EndOfComputation -> strategy.reachedEndOfComputation(computation, this)
            is SamplingComputation<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                strategy.sampleBranching(computation as SamplingComputation<T, Any>, this)
            }
        }
    }

    internal fun enterComputation() {
        logFactorsStack.push(currentLogProbability)
    }

    internal fun exitComputation() {
        val logFactorToReturn = logFactorsStack.pop()
        currentLogProbability = logFactorToReturn
    }

    fun factor(factoringFunction: () -> Double) {
        val logProbability = factoringFunction()
        currentLogProbability += logProbability
    }

    fun observe(evidence: () -> Boolean) =
        factor { if (evidence()) 0.0 else Double.NEGATIVE_INFINITY }

    fun <T, R> sample(
        from: Distribution<R>,
        continuation: ProbabilisticContext<T>.(R) -> ProbabilisticComputation<T>
    ): ProbabilisticComputation<T> =
        SamplingComputation(from, continuation)
}

fun <T> ProbabilisticContext<T>.endWith(result: T) = EndOfComputation(result)

class EndOfComputation<T>(val value: T) : ProbabilisticComputation<T>()

class SamplingComputation<T, R>(
    val distributionToSample: Distribution<R>,
    val continuation: ProbabilisticContext<T>.(R) -> ProbabilisticComputation<T>
) : ProbabilisticComputation<T>() {
}

abstract class InferenceStrategy<T>() {
    abstract fun reachedEndOfComputation(
        endOfComputation: EndOfComputation<T>,
        probabilisticContext: ProbabilisticContext<T>
    )

    abstract fun <R> sampleBranching(
        samplingComputation: SamplingComputation<T, R>,
        probabilisticContext: ProbabilisticContext<T>
    )
}

fun <T> Map<T, Double>.normalize(): Map<T, Double> {
    val sumValues = values.sum()
    return mapValues { (_, v) -> v / sumValues }
}

fun <T> simplyMarginalize(probabilisticProgram: ProbabilisticProgram<T>): Map<T, Double> {
    val strategy = SimpleMarginalize<T>()
    val context = ProbabilisticContext(strategy)
    context.handleComputation(probabilisticProgram.invoke(context))
    return strategy.results.normalize()
}

class SimpleMarginalize<T> : InferenceStrategy<T>() {
    internal val results = mutableMapOf<T, Double>()

    override fun reachedEndOfComputation(
        endOfComputation: EndOfComputation<T>,
        probabilisticContext: ProbabilisticContext<T>
    ) {
        val resultValue = endOfComputation.value
        val resultProbability = Math.exp(probabilisticContext.currentLogProbability)
        results.merge(resultValue, resultProbability, Double::plus)
    }

    override fun <R> sampleBranching(
        samplingComputation: SamplingComputation<T, R>,
        probabilisticContext: ProbabilisticContext<T>
    ) {
        val distribution = samplingComputation.distributionToSample
        for (r in distribution.support) {
            with(probabilisticContext) {
                enterComputation()
                factor { distribution.score(r) }
                handleComputation(samplingComputation.continuation(probabilisticContext, r))
                exitComputation()
            }
        }
    }
}