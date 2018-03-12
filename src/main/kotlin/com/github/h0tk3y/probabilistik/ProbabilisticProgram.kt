package com.github.h0tk3y.probabilistik

class ProbabilisticProgram<T>(
    val computation: suspend ProbabilisticContext<*, *>.() -> T
)