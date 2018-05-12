import com.github.h0tk3y.probabilistik.distribution.Gaussian
import com.github.h0tk3y.probabilistik.distribution.Uniform
import com.github.h0tk3y.probabilistik.inference.ParticleFilterStrategy
import com.github.h0tk3y.probabilistik.inference.SampleOnceStrategy
import com.github.h0tk3y.probabilistik.inference.ValueWithProbability
import com.github.h0tk3y.probabilistik.inference.infer
import com.github.h0tk3y.probabilistik.probabilistic
import org.junit.Assert
import org.junit.Test

class TestDistributions {
    @Test
    fun testSimpleGaussian() {
        val d1 = Gaussian(1.0, 2.0)
        val d2 = Gaussian(6.0, 2.0)
        val d3 = Gaussian(7.0, 2.0)
        val expectedMean = listOf(d1, d2, d3).sumByDouble { it.mean }
        val program = probabilistic {
            val x = sample(d1)
            val y = sample(d2)
            val z = sample(d3)
            x + y + z
        }
        val samples = (1..1000).map { program.infer(SampleOnceStrategy()) }
        val mean = samples.map { (it as ValueWithProbability).value }.average()
        println("mean = $mean")
        Assert.assertTrue(mean in expectedMean vicinity 0.5)
    }

    @Test
    fun testSimpleUniform() {
        val d1 = Uniform(0.0..1.0)
        val d2 = Uniform(2.0..3.0)
        val d3 = Uniform(10.0..20.0)
        val expectedMean = listOf(d1, d2, d3).map { it.range.run { start + endInclusive } / 2 }.sum()
        val program = probabilistic {
            val x = sample(d1)
            val y = sample(d2)
            val z = sample(d3)
            x + y + z
        }
        val samples = program.infer(ParticleFilterStrategy(100))
        val mean = samples.average()
        println("mean = $mean")
        Assert.assertTrue(mean in expectedMean vicinity 0.5)
    }
}