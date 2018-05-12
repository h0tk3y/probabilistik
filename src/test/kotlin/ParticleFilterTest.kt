
import com.github.h0tk3y.probabilistik.distribution.*
import com.github.h0tk3y.probabilistik.flip
import com.github.h0tk3y.probabilistik.inference.DistributionSampler
import com.github.h0tk3y.probabilistik.inference.ParticleFilterStrategy
import com.github.h0tk3y.probabilistik.inference.infer
import com.github.h0tk3y.probabilistik.normalize
import com.github.h0tk3y.probabilistik.probabilistic
import org.junit.Assert
import org.junit.Test

class ParticleFilterTest {
    @Test
    fun testSimple() {
        val program = probabilistic {
            val distribution = Multinomial((1..10).toList()) { it.toDouble() }
            val b1 = sample(distribution)
            observe { b1 >= 5 }
            b1
        }
        val nSamples = 100000
        val result = program.infer(ParticleFilterStrategy(nSamples))
        Assert.assertEquals(result.size, nSamples)
        val probabilities = result.groupingBy { it }.eachCount().normalize()
        Assert.assertEquals((5..10).toList(), probabilities.keys.sorted())
    }

    @Test
    fun testTwoNormalDistributionsFromOne() {
        val original = Gaussian(0.0, 20.0)
        val m1 = 3.0
        val m2 = -3.0
        val v1 = 1.0
        val v2 = 2.0
        val target1 = Gaussian(m1, v1)
        val target2 = Gaussian(m2, v2)
        val program = probabilistic {
            val s = sample(original)
            val f = flip(0.5)
            factor { if (f) target1.logLikelihood(s) else 0.0 }
            factor { if (f) 0.0 else target2.logLikelihood(s) }
            f to s
        }
        val nSamples = 10000
        val result = program.infer(ParticleFilterStrategy(nSamples, object : DistributionSampler {
            override fun <T> howToSample(distribution: Distribution<T>): Iterable<T>? =
                when (distribution) {
                    is Bernoulli -> (distribution as FiniteDistribution).support
                    else -> null
                }
        }))

        Assert.assertEquals(2 * nSamples, result.size)
        val (t1, t2) = result.partition { (f, _) -> f }
        val t1Mean = t1.map { (_, s) -> s }.average()
        val t2Mean = t2.map { (_, s) -> s }.average()
        val t1Variance = t1.map { (_, s) -> s * s }.average() - t1Mean * t1Mean
        val t2Variance = t2.map { (_, s) -> s * s }.average() - t2Mean * t2Mean
        println("t1m = $t1Mean, t2m = $t2Mean, t1v = $t1Variance, t2v = $t2Variance")
        Assert.assertTrue(t1Mean in m1 vicinity 0.5)
        Assert.assertTrue(t2Mean in m2 vicinity 0.5)
        Assert.assertTrue(t1Variance in v1 vicinity 0.5)
        Assert.assertTrue(t2Variance in v2 vicinity 0.5)
    }

}