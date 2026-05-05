/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fxboomk.fcitx5.android.input.candidates.pagedvertical

import android.graphics.drawable.GradientDrawable
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import org.fxboomk.fcitx5.android.core.FcitxEvent
import org.fxboomk.fcitx5.android.data.theme.Theme
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.textView

class PagedVerticalCandidateItemUi(
    override val ctx: android.content.Context,
    val theme: Theme
) : Ui {

    val text = textView {
        isSingleLine = true
        gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
        setTextColor(theme.candidateTextColor)
        setPadding(4, 0, 4, 0)
    }

    private val highlightDrawable = GradientDrawable().apply {
        setColor(theme.genericActiveBackgroundColor)
        cornerRadius = ctx.resources.displayMetrics.density * 4
    }

    fun update(candidate: FcitxEvent.Candidate, active: Boolean) {
        val labelFg = if (active) theme.genericActiveForegroundColor else theme.candidateLabelColor
        val fg = if (active) theme.genericActiveForegroundColor else theme.candidateTextColor
        val altFg = if (active) theme.genericActiveForegroundColor else theme.candidateCommentColor
        text.text = buildSpannedString {
            color(labelFg) { append(candidate.label) }
            color(fg) { append(candidate.text) }
            if (candidate.comment.isNotBlank()) {
                append(" ")
                color(altFg) { append(candidate.comment) }
            }
        }
        root.background = if (active) highlightDrawable else null
    }

    override val root = text
}

