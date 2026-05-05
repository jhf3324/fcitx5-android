/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fxboomk.fcitx5.android.input.candidates.pagedvertical

import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.fxboomk.fcitx5.android.R
import org.fxboomk.fcitx5.android.core.CapabilityFlags
import org.fxboomk.fcitx5.android.core.FcitxEvent
import org.fxboomk.fcitx5.android.core.FcitxEvent.PagedCandidateEvent
import org.fxboomk.fcitx5.android.daemon.launchOnReady
import org.fxboomk.fcitx5.android.input.bar.ExpandButtonStateMachine
import org.fxboomk.fcitx5.android.input.bar.KawaiiBarComponent
import org.fxboomk.fcitx5.android.input.broadcast.InputBroadcastReceiver
import org.fxboomk.fcitx5.android.input.dependency.UniqueViewComponent
import org.fxboomk.fcitx5.android.input.dependency.context
import org.fxboomk.fcitx5.android.input.dependency.fcitx
import org.fxboomk.fcitx5.android.input.dependency.inputMethodService
import org.fxboomk.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.manager.must
import splitties.dimensions.dp

class PagedVerticalCandidatesComponent :
    UniqueViewComponent<PagedVerticalCandidatesComponent, LinearLayout>(),
    InputBroadcastReceiver {

    private val service by manager.inputMethodService()
    private val context by manager.context()
    private val fcitx by manager.fcitx()
    private val theme by manager.theme()
    private val bar: KawaiiBarComponent by manager.must()

    private val gridSpacing: Int by lazy { context.dp(1) }
    private val panelSidePadding: Int by lazy { context.dp(1) }
    private val fixedFontSizeSp: Float by lazy { 14f }
    private val fixedItemHeightPx: Int by lazy {
        val barHeightPx = context.dp(KawaiiBarComponent.HEIGHT)
        val availableHeight = barHeightPx - context.dp(2)
        (availableHeight / 2).coerceAtLeast(context.dp(18))
    }
    private val columnCount: Int by lazy {
        val screenWidthDp = context.resources.displayMetrics.widthPixels /
            context.resources.displayMetrics.density
        when {
            screenWidthDp >= 480 -> 6
            screenWidthDp >= 360 -> 5
            else -> 4
        }
    }

    private var pagedData = PagedCandidateEvent.Data.Empty
    private var isPagedMode = false
    private var hasContent = false
    private val adapter by lazy {
        PagedVerticalCandidatesAdapter(
            theme, fixedFontSizeSp, fixedItemHeightPx,
            onItemClick = { index: Int -> onCandidateClick(index) },
            onPrevPage = { onPrevPage() },
            onNextPage = { onNextPage() }
        )
    }

    private val layoutManager by lazy {
        FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            alignItems = AlignItems.FLEX_START
            justifyContent = JustifyContent.FLEX_START
        }
    }

    private val contentLayout: LinearLayout by lazy {
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            val recycler = object : RecyclerView(context) {
                override fun onTouchEvent(e: android.view.MotionEvent) = false
                override fun onInterceptTouchEvent(e: android.view.MotionEvent) = false
            }.apply {
                adapter = this@PagedVerticalCandidatesComponent.adapter
                layoutManager = this@PagedVerticalCandidatesComponent.layoutManager
                setHasFixedSize(false)
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isNestedScrollingEnabled = false
                itemAnimator = null  // Disable animations for smooth fast typing
                setPadding(panelSidePadding, 0, panelSidePadding, 0)
                addItemDecoration(PagedVerticalItemDecoration(gridSpacing))
            }

            addView(recycler, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    private val _root: LinearLayout by lazy {
        LinearLayout(context).apply {
            minimumHeight = context.dp(KawaiiBarComponent.HEIGHT)
            addView(contentLayout, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
    }

    override val view: LinearLayout get() = _root

    fun onPrevPage() {
        if (isPagedMode && pagedData.hasPrev) {
            fcitx.launchOnReady { it.offsetCandidatePage(-1) }
        }
    }

    fun onNextPage() {
        if (isPagedMode && pagedData.hasNext) {
            fcitx.launchOnReady { it.offsetCandidatePage(1) }
        }
    }

    override fun onStartInput(info: EditorInfo, capFlags: CapabilityFlags) {
        isPagedMode = false
        pagedData = PagedCandidateEvent.Data.Empty
        adapter.reset()
        view.post { hide() }
    }

    override fun onPagedCandidateUpdate(data: PagedCandidateEvent.Data) {
        if (data == PagedCandidateEvent.Data.Empty) {
            view.post { hide() }
            return
        }
        isPagedMode = true
        pagedData = data
        adapter.updatePagedCandidates(data)
        view.post { show() }
    }

    override fun onCandidateUpdate(data: FcitxEvent.CandidateListEvent.Data) {
        if (isPagedMode) return
        if (data.total <= 0 || data.candidates.isEmpty()) {
            view.post { hide() }
            return
        }
        adapter.updateLegacyCandidates(data)
        view.post { show() }
    }

    private fun show() {
        if (view.visibility != View.VISIBLE) {
            view.visibility = View.VISIBLE
            val lp = view.layoutParams
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            view.layoutParams = lp
            hasContent = true
            bar.expandButtonStateMachine.push(
                ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesUpdated,
                ExpandButtonStateMachine.BooleanKey.ExpandedCandidatesEmpty to false
            )
        }
    }

    private fun hide() {
        if (view.visibility != View.INVISIBLE) {
            view.visibility = View.INVISIBLE
            val lp = view.layoutParams
            lp.height = context.dp(KawaiiBarComponent.HEIGHT)
            view.layoutParams = lp
            hasContent = false
            isPagedMode = false
            pagedData = PagedCandidateEvent.Data.Empty
            bar.expandButtonStateMachine.push(
                ExpandButtonStateMachine.TransitionEvent.ExpandedCandidatesUpdated,
                ExpandButtonStateMachine.BooleanKey.ExpandedCandidatesEmpty to true
            )
        }
    }

    private fun onCandidateClick(index: Int) {
        if (isPagedMode) {
            fcitx.launchOnReady { it.select(index) }
        } else {
            fcitx.launchOnReady { it.select(adapter.indexOffset + index) }
        }
    }
}
