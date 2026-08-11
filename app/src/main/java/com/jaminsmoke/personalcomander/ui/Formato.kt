package com.jaminsmoke.personalcomander.ui

import java.util.Locale

private val EURO_FORMAT_LOCALE = Locale("es", "ES")

internal fun Double.formatoEuro(): String = String.format(EURO_FORMAT_LOCALE, "%.2f €", this)
