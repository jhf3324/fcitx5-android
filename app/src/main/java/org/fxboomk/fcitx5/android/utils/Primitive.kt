/*  * SPDX-License-Identifier: LGPL-2.1-or-later  * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors  */  package org.fxboomk.fcitx5.android.utils  import androidx.core.graphics.ColorUtils import kotlin.math.roundToInt  fun Int.alpha(a: Float) = ColorUtils.setAlphaComponent(this, (a * 0xff).roundToInt()) 

fun android.content.Context.styledFloat(attr: Int): Float {
    val ta: android.content.res.TypedArray = theme.obtainStyledAttributes(intArrayOf(attr))
    val value = ta.getFloat(0, 0.0f)
    ta.recycle()
    return value
}