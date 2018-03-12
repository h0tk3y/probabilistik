package com.github.h0tk3y.probabilistik

inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    val result = arrayListOf<R>()
    val original = toList()
    for (i in 0..original.lastIndex) {
        val element = transform(original[i])
        while (result.size > i)
            result.removeAt(result.lastIndex)
        result.add(element)
    }
    return result
}

inline fun <T> Iterable<T>.each(action: (T) -> Unit) {
    val original = toList()
    for (i in original.indices) {
        action(original[i])
    }
}

inline fun <T, R> Iterable<T>.fold(initialValue: R, step: (R, T) -> R): R {
    val original = toList()
    var currentStep = initialValue
    for (i in original.indices) {
        val nextStep = step(currentStep, original[i])
        currentStep = nextStep
    }
    return currentStep
}

inline fun <T> Iterable<T>.reduce(step: (T, T) -> T) = drop(1).fold(first(), step)