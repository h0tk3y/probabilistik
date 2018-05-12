probabilistik
===

[ ![Download](https://api.bintray.com/packages/hotkeytlt/maven/probabilistik/images/download.svg) ](https://bintray.com/hotkeytlt/maven/probabilistik/_latestVersion) [![Build Status](https://travis-ci.com/h0tk3y/probabilistik.svg?branch=master)](https://travis-ci.com/h0tk3y/probabilistik)

This is a proof-of-concept implementation of a probabilistic programming framework
for Kotlin, using the Kotlin coroutines. It is mostly inspired by 
[The Design and Implementation of Probabilistic Programming Languages](http://dippl.org). 

**Disclaimer**: This implementation uses a sort of really dark magic to circumvent
the limitation of the Kotlin coroutine continuation that can run only once. This 
framework can run the continuations several times, rolling back their state (label + local variables)
to the point where it needs to continue. The probabilistic functions are therefore hostile
to mutable state, and you are advised not to use mutable state at all, even that hidden in 
inline functions that you pass a suspending lambda (like `map { ... }` from the Kotlin stdlib). 
If you do, you may end up with a computation rolled back with the state still modified by another branch of the computation.
This library is purely experimental and is by no means production-ready. The author is not 
to be held responsible if the code breaks, or the code breaks your computer, or the code kills your neighbor's dog.

### Adding to Gradle Project

``` groovy
repositories {
    maven { url 'https://dl.bintray.com/hotkeytlt/maven' }
}

dependencies {
    compile 'com.github.h0tk3y.kotlin.probabilistik:probabilistik:0.1'
}
```

### Introduction

Probabilistic computations are similar to normal computations that use random values from some distributions 
but differ in one respect. Whenever a normal computation takes a random value, it continues to run with that
single value. But when a probabilistic computation accesses a random variable, it only means that
it continues with *some* value from the distribution. Basically, you can think of probabilistic programs as of 
observations of all possible runs of normal computations that work with distributions.

Example:

```
val program = probabilistiic {
    val coin1 = flip()
    val coin2 = flip()
    println("$coin1 $coin2")
}
```

This piece of code *observes* the ways a computation can toss a coin two times 
(ending up with four possible options at the point where `println` happens).

Probabilistic computations can also be considered a tool that allows making certain
judgement about computations that work with random variables. This judgement is obtained
during a process called inference *inference* (described below), but first, we'll discuss
what we mean by *observing* a probabilistic computation.

### Factorization and observation

In the course of a probabilistic computation, the actions it may encounter that distinguish it 
from normal computation are sampling and factorization. While the former deals with 
random variables, the latter can be used to express the knowledge about the outcomes, or,
equivalently, to choose the outcomes that we want to observe (and to filter the ones that 
we are not interested in).

Once a probabilistic computation reaches a factorization operation, it modifies the posterior
probability of the current outcome. Example:

```
val dice = randomInteger(1..6)
val program = probabilistic  {
    val a = sample(dice)
    observe { a in 1..2 }
    val b = sample(dice)
    observe { a + b >= 7 }
    b
}
```

> Here, `observe { ... }` is a simple factorization operation that sets probability of the 
outcome where it happens to zero if the condition is not satisfied. More complex factorization
scenarios can be implemented by using a more general `factor { ... }` function that can modify
the probability of the outcome arbitrarily.

This computation observes double rolls of a dice, while taking into account only those 
rolls of the first dice that resulted into 1 or 2 that was followed by a second roll 
that adds up to the score of 7 or more (when the first roll is 1, it can only be 6, 
and with 2 for the first roll, it can be 5 or 6).

Without the second `observe { ... }` statement, the rolls of the second dice would result 
into 1 to 6 with equal probabilities, but the second statement modifies the posterior 
probability for values of `b` so that they become: 5 → 0.333..., 6 → 0.666. This knowledge
of posterior probability is one thing that can be obtained from a probabilistic computation
by the process we call *inference*.

### Inference

Once you define a probabilistic program, you don't have a direct way to run it, because
the semantics of a probabilistic computation intentionally leave the random values choice 
undefined, so a probabilistic computation itself doesn't know what it should do when it encounters a 
random choice instruction.

The semantics is, though, completed by inference strategies, which are exactly definitions of 
what should happen when a computation encounters a random choice or a factoring operation, 
while a probabilistic computation intentionally abstracts itself away from this aspect.

Some examples of inference strategies are:

* Run as a normal computation, just sampling a single value when asked a value from a distribution (not exploring
 the random choices in any other way). This example shows that normal computations are a subset of
 probabilistic computations;
 
* Run the computation taking N random sample values when the computation asks for a 
 random variable sample, then collect and count the results – this strategy can provide 
 an estimation on the distribution of the results that the computation reaches with those sample values;
 
* If the value sets of the distributions are finite, you can consider a strategy that
 enumerates all possible results and thus builds a multinomial distribution of possible outcomes.
 
* [Particle filtering](http://dippl.org/chapters/05-particlefilter.html) can be seen as an advanced inference 
 strategy that is suitable for approximate inference in complex scenarios (such as in the 
 [Regression.kt](https://github.com/h0tk3y/probabilistik/blob/master/src/test/kotlin/Regression.kt) demo) 
 
An inference strategy provides a resulting value, which has its semantics depending on the strategy
itself. In the examples above, it might be a probabilistic distribution, a single value (possibly, with its likelihood) or 
a set of values.

Once you have a probabilistic program, you can run inference on it by calling `.infer(strategy)` where `strategy` is an 
`InferenceStrategy` implementation:

``` kotlin
val program = probabilistic {
    val d1 = flip(0.5)
    val d2 = flip(0.5)
    observe { d1 || d2 }
    d1
}
val result: Map<Boolean, Double> = program.infer(EnumerateStrategy()) 
```

There are several inference strategies already implemented in this library (see [`src/main/kotlin/.../inference`](https://github.com/h0tk3y/probabilistik/tree/master/src/main/kotlin/com/github/h0tk3y/probabilistik/inference)):

* `SampleOnceStrategy` samples each value only once, and its result is a single value (or a special `Impossible` value 
if it finds no way to continue computation)

* `EnumerateStrategy` enumerates all possible outcomes with their probabilities and thus requires the
 all distributions to be instances of `FiniteDistribution`; the result is a multinomial distribution of the outcomes
 
* `WeightedQueueStrategy` is similar to `EnumerateStrategy`, but it visits outcomes that are more probable 
 first and therefore allows for finding approximate solutions by dropping less probable outcomes to reduce
 the computation complexity
 
* `ParticleFilterStrategy` is an implementation of particle filtering, which maintains a set of samples and 
 performs resampling them upon encountering a factoring operation (so that the samples set retains the 
 items in quantities proportional to their likelihood), and yields the resulting set of samples
 
### Examples

See [`src/tests/kotlin`](https://github.com/h0tk3y/probabilistik/tree/master/src/test/kotlin) for usage examples.
