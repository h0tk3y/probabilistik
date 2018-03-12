
import com.github.h0tk3y.probabilistik.distribution.randomInteger
import com.github.h0tk3y.probabilistik.each
import com.github.h0tk3y.probabilistik.flip
import com.github.h0tk3y.probabilistik.inference.enumerate
import com.github.h0tk3y.probabilistik.probabilistic
import org.junit.Assert
import org.junit.Test

class SimpleTests {
    @Test
    fun testTwoCoins() {
        val program = probabilistic {
            val d1 = flip(0.5)
            val d2 = flip(0.5)
            observe { d1 || d2 }
            d1
        }
        val posterior = program.enumerate()
        Assert.assertEquals(mapOf(true to 2.0 / 3, false to 1.0 / 3), posterior)
    }

    @Test
    fun testIntSum() {
        val program = probabilistic {
            val d = randomInteger(1..6)
            var sum = 0
            (1..3).each { sum += sample(d) }
            sum
        }
        val posterior = program.enumerate()
        Assert.assertEquals((3..18).toList(), posterior.keys.toList())
        Assert.assertEquals(posterior.values.toList().take(8), posterior.values.toList().drop(8).reversed())
    }

    @Test
    fun testSimpleDice() {
        val dice = randomInteger(1..6)
        val program = probabilistic {
            val a = sample(dice)
            observe { a in 1..2 }
            val b = sample(dice)
            observe { a + b >= 7 }
            b
        }
        val posterior = program.enumerate()
        Assert.assertEquals(mapOf(6 to 2.0 / 3, 5 to 1.0 / 3), posterior)
    }
}