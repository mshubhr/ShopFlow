package com.shopflow.ui.components

import java.text.NumberFormat
import java.util.Locale

fun price(amount: Double): String = (ThreadLocal.withInitial {
    NumberFormat.getCurrencyInstance(Locale.US)
}.get() ?: NumberFormat.getCurrencyInstance(Locale.US)).format(amount)
