package com.jaminsmoke.personalcomander.ui

import java.util.Locale

internal fun Double.formatoEuro(): String = String.format(Locale.getDefault(), "%.2f €", this)
