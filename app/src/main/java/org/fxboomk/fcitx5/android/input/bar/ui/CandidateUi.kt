/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */

package org.fxboomk.fcitx5.android.input.bar.ui

import android.content.Context
import splitties.views.dsl.core.Ui

/**
 * Bar UI shown when there are candidates.
 * Empty - all UI elements have been removed.
 * Preedit text is displayed in its own separate area (PreeditComponent).
 */
class CandidateUi(override val ctx: Context) : Ui {

    override val root = android.widget.Space(ctx)
}

