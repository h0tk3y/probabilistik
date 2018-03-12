import com.github.h0tk3y.probabilistik.distribution.Gaussian
import com.github.h0tk3y.probabilistik.inference.SampleOnceStrategy
import com.github.h0tk3y.probabilistik.inference.infer
import com.github.h0tk3y.probabilistik.probabilistic
import org.junit.Test

class TestGaussian {
    @Test
    fun testSimpleGaussian() {
        val d1 = Gaussian(1.0, 2.0)
        val d2 = Gaussian(5.0, 2.0)
        val program = probabilistic {
            val x = sample(d1)
            val y = sample(d2)
            x + y
        }
        for (i in 1..10) {
            println(program.infer(SampleOnceStrategy()))
        }
    }
}