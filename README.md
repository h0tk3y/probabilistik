probabilistik
===

This is a proof-of-concept implementation of a probabilistic programming framework
for Kotlin, using the Kotlin coroutines.

**Disclaimer**: This implementation uses a sort of really dark magic to circumvent
the limitation of the Kotlin coroutine continuation that can run only once. This 
framework can run the continuations several times, rolling back their state (label + local variables)
to the point where it needs to continue. The probabilistic functions are therefore hostile
to mutable state, and you are advised not to use mutable state at all, even that hidden in 
inline functions that you pass a suspending lambda (like `map { ... }` from the Kotlin stdlib). 
If you do, you may end up with a computation rolled back with the state still modified by another branch of the computation.
This library is purely experimental and is by no means production-ready. The author is not 
to be held responsible if the code breaks, or the code breaks your computer, or the code kills your neighbor's dog.

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

### Observation

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
what should happen when a computation encounters a random choice, while a probabilistic computation 
itself abstracts from this aspect.

Some examples of inference strategies are:

* Run as a normal computation, just sampling a single value when asked a value from a distribution (not exploring
 the random choices in any other way). This example shows that normal computations are a subset of
 probabilistic computations;
 
* Run the computation taking N random sample values when the computation asks for a 
 random variable sample, then collect and count the results – this strategy can provide 
 an estimation on the distribution of the results that the computation reaches with those sample values;
 
* If the value sets of the distributions are finite, you can consider a strategy that
 enumerates all possible results  
 
An inference strategy provides a resulting value, which has its semantics depending on the strategy
itself. In the examples above, it might be a probabilistic distribution, a single value (possibly, with its likelihood) or 
a set of values.