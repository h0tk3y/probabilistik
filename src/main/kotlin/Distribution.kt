import java.util.*

interface Distribution<T> {
    fun score(value: T): Double

    val support: Iterable<T>
}

private val random =
    System.getProperty("probabilistic.seed")?.toLongOrNull()?.let { Random(it) }
    ?: Random()

class Bernoulli(val p: Double) : Distribution<Boolean> {
    init {
        require(p in 0.0..1.0) { "The probability 'p' should be inside 0..1" }
    }

    override fun score(value: Boolean): Double = Math.log(if (value) p else 1.0 - p)

    override val support: Set<Boolean> = setOf(true, false)
}

fun <T> ProbabilisticContext<T>.flip(
    p: Double,
    continuation: ProbabilisticContext<T>.(Boolean) -> ProbabilisticComputation<T>
): ProbabilisticComputation<T> {
    val distribution = Bernoulli(p)
    return sample(distribution, continuation)
}

class Multinomial<T>(val probabilities: Map<T, Double>) : Distribution<T> {
    override fun score(value: T): Double = probabilities[value] ?: 0.0

    override val support: Iterable<T>
        get() = probabilities.keys
}