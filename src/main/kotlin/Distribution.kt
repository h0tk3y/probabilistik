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