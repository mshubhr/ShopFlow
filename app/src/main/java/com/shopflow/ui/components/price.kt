package com.shopflow.ui.components

import java.text.NumberFormat
import java.util.Locale

private val currencyFormat = ThreadLocal.withInitial {
    NumberFormat.getCurrencyInstance(Locale.US)
}

fun price(amount: Double): String = currencyFormat.get()!!.format(amount)
