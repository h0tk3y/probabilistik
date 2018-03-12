package com.github.h0tk3y.probabilistik.inference

import com.github.h0tk3y.probabilistik.ContinuationBranching
import com.github.h0tk3y.probabilistik.ProbabilisticContext
import com.github.h0tk3y.probabilistik.distribution.Distribution
import com.github.h0tk3y.probabilistik.distribution.Multinomial
import java.util.*

interface DistributionSampler {
    /** Overrides the sampling algorithm for a single distribution. Returns null to use the default sampling algorihtm
     * or a list of samples from the distribution otherwise. */
    fun <T> howToSample(distribution: Distribution<T>): Iterable<T>?
}

private object SampleByDefault : DistributionSampler {
    override fun <T> howToSample(distribution: Distribution<T>): Iterable<T>? = null
}

class ParticleFilterStrategy<T>(
    val nParticlesPerSample: Int,
    val samplingOverride: DistributionSampler = SampleByDefault
) : InferenceStrategy<T, List<T>> {

    private class Particle<R>(
        val branching: ContinuationBranching<R>,
        val logProbability: Double,
        val valueToContinue: R
    )

    private fun <R> register(particle: Particle<R>) {
        particles.add(particle)
        particlesQueue.add(particle)
    }

    // Use a stub for the initial Particle:
    private var currentParticle: Particle<*> =
        Particle(object : ContinuationBranching<Unit> {
            override fun runBranch(withValue: Unit, beforeResume: () -> Unit, afterResume: () -> Unit) {}
        }, 0.0, Unit)

    private val particles: MutableSet<Particle<*>> = LinkedHashSet<Particle<*>>().apply { add(currentParticle) }
    private val particlesQueue = ArrayDeque<Particle<*>>().apply { addFirst(currentParticle) }

    private val factoredParticleWeights = mutableMapOf<Particle<*>, Double>()

    private val factoredParticleContinuations = mutableMapOf<Particle<*>, ContinuationBranching<Unit>>()

    override fun reachedEndOfComputation(
        returnValue: T,
        probabilisticContext: ProbabilisticContext<T, List<T>>
    ) {
        resultingValues += returnValue
    }

    private val resultingValues = mutableListOf<T>()

    override val result: List<T>
        get() = resultingValues

    override fun factoring(
        branching: ContinuationBranching<Unit>,
        factoringFunction: () -> Double,
        probabilisticContext: ProbabilisticContext<T, List<T>>
    ) {
        check(currentParticle !in factoredParticleWeights)
        factoredParticleContinuations[currentParticle] = branching
        val factoringLogProbability = factoringFunction()
        factoredParticleWeights[currentParticle] = currentParticle.logProbability + factoringLogProbability

        if (factoredParticleWeights.size == particles.size) {
            resample()
        }

        handleParticles(probabilisticContext)
    }

    private fun resample() {
        val newParticlesDistribution = Multinomial(particles.toList()) { Math.exp(factoredParticleWeights[it]!!) }
        val newParticles = List(particles.size) {
            val p = newParticlesDistribution.takeSample()
            Particle(factoredParticleContinuations[p]!!, 0.0, Unit)
        }
        particles.clear()
        factoredParticleWeights.clear()
        factoredParticleContinuations.clear()
        newParticles.forEach { register(it) }
    }

    private var inHandleLoop = false

    private fun handleParticles(probabilisticContext: ProbabilisticContext<T, List<T>>) {
        if (!inHandleLoop) {
            inHandleLoop = true
            while (particlesQueue.isNotEmpty()) {
                val particle = particlesQueue.remove()
                stepWithParticle(particle, probabilisticContext)
            }
            inHandleLoop = false
        }
    }

    private fun <R> stepWithParticle(
        particle: Particle<R>,
        probabilisticContext: ProbabilisticContext<T, List<T>>
    ) {
        currentParticle = particle
        probabilisticContext.currentLogProbability = particle.logProbability
        particle.branching.runBranch(particle.valueToContinue)
    }

    override fun <R> sampleBranching(
        branching: ContinuationBranching<R>,
        distributionToSample: Distribution<R>,
        probabilisticContext: ProbabilisticContext<T, List<T>>
    ) {
        particles.remove(currentParticle)

        val samples = samplingOverride.howToSample(distributionToSample)
                      ?: List(nParticlesPerSample) { distributionToSample.takeSample() }

        samples.map { sampleValue ->
            Particle(branching, currentParticle.logProbability, sampleValue).also { register(it) }
        }

        handleParticles(probabilisticContext)
    }
}