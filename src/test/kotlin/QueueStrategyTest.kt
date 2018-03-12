
import com.github.h0tk3y.probabilistik.distribution.Multinomial
import com.github.h0tk3y.probabilistik.inference.WeightedQueueStrategy
import com.github.h0tk3y.probabilistik.inference.infer
import com.github.h0tk3y.probabilistik.map
import com.github.h0tk3y.probabilistik.normalize
import com.github.h0tk3y.probabilistik.probabilistic
import org.junit.Assert
import org.junit.Test

class QueueStrategyTest {
    @Test
    fun testSimple() {
        val b1 = Multinomial(listOf(1, 2, 3)) { it * 100.0 }
        val b2 = Multinomial(listOf(1, 2, 3)) { 500.0 - it }
        val intermediate = mutableListOf<Int>()
        val observations = mutableListOf<Pair<Int, Int>>()

        val program = probabilistic {
            val d1 = sample(b1).also { intermediate.add(it) }
            (d1 to sample(b2)).also { observations.add(it) }
        }

        val strategy = WeightedQueueStrategy<Pair<Int, Int>>()
        val result = program.infer(strategy).normalize()
        Assert.assertEquals(listOf(3, 2, 1), intermediate)
        Assert.assertEquals(result.entries.sortedBy { -it.value }.map { it.key }, observations)
    }
}