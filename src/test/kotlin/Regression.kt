import com.github.h0tk3y.probabilistik.distribution.Uniform
import com.github.h0tk3y.probabilistik.each
import com.github.h0tk3y.probabilistik.inference.ParticleFilterStrategy
import com.github.h0tk3y.probabilistik.inference.infer
import com.github.h0tk3y.probabilistik.probabilistic
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

class Regression {
    @Test
    fun testRegression() {
        val random = Random()

        val c1 = random.nextDouble()
        val c2 = random.nextDouble()

        fun expSimilarity(d1: Double, d2: Double, scale: Double = 1.0) =
            -1.0 * exp(abs(d1 - d2) * scale)

        val nData = 1000

        val noiseFactor = 0.02

        val data = (1..nData).map {
            val x1 = random.nextDouble()
            val noise = random.nextGaussian() * noiseFactor
            x1 to c1 * x1 * x1 + c2 * x1 + noise
        }

        val qDistribution = Uniform(0.0..1.0)

        val program = probabilistic {
            val q1 = sample(qDistribution)
            val q2 = sample(qDistribution)
            val answers = data.map { (x1, _) -> q1 * x1 * x1 + q2 * x1 }
            data.indices.each {
                val (_, r) = data[it]
                val e = answers[it]
                factor { expSimilarity(e, r, 10.0) }
            }
            q1 to q2
        }
        
        println("Polynom: $c1 * x^2 + $c2 * x")
        val result = program.infer(ParticleFilterStrategy(20))
        val r1 = result.map { it.first }.average()
        val r2 = result.map { it.second }.average()
        val dataRange = data.maxBy { it.first }!!.second - data.minBy { it.second }!!.second
        val nrmse = sqrt(data.map { (x1, r) ->
            (r - (x1 * x1 * r1 + r2 * x1)).let { it * it }
        }.average()) / dataRange

        println("Result: $r1 * x^2 + $r2 * x")
        println("NRMSE: $nrmse")

        Assert.assertTrue(nrmse <= 0.1)
    }
}