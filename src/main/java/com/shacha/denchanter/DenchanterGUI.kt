package com.shacha.denchanter

infix fun <F, S> F.of(that: S): MutablePair<F, S> = MutablePair(this, that)