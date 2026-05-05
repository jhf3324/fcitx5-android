/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fxboomk.fcitx5.android.input.candidates.pagedvertical

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import org.fxboomk.fcitx5.android.R
import org.fxboomk.fcitx5.android.core.FcitxEvent
import org.fxboomk.fcitx5.android.core.FcitxEvent.CandidateListEvent
import org.fxboomk.fcitx5.android.core.FcitxEvent.PagedCandidateEvent
import org.fxboomk.fcitx5.android.data.theme.Theme
import org.fxboomk.fcitx5.android.utils.styledFloat
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.imageDrawable

sealed class PagedVcViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class Candidate(val ui: PagedVerticalCandidateItemUi) : PagedVcViewHolder(ui.root)
    class Pagination(val prevIcon: ImageView, val nextIcon: ImageView, container: View) :
        PagedVcViewHolder(container)
}

class PagedVerticalCandidatesAdapter(
    private val theme: Theme,
    private val fontSizeSp: Float,
    private val itemHeightPx: Int,
    private val onItemClick: (Int) -> Unit,
    private val onPrevPage: () -> Unit,
    private val onNextPage: () -> Unit
) : RecyclerView.Adapter<PagedVcViewHolder>() {

    init {
        setHasStableIds(true)
    }

    companion object {
        private const val VIEW_TYPE_CANDIDATE = 0
        private const val VIEW_TYPE_PAGINATION = 1
        private const val ARROW_WIDTH_DP = 7
        private const val ARROW_HEIGHT_DP = 16
    }

    private var pagedCandidates: Array<FcitxEvent.Candidate> = emptyArray()
    private var activeIndex = -1
    private var isPagedMode = false
    private var currentFontSizeSp: Float = fontSizeSp
    private var hasArrows = false
    private var hasPrev = false
    private var hasNext = false

    var candidates: Array<String> = emptyArray()
        private set
    var total = -1
        private set
    var indexOffset = 0
        private set

    fun updatePagedCandidates(data: PagedCandidateEvent.Data) {
        pagedCandidates = data.candidates
        activeIndex = data.cursorIndex
        isPagedMode = true
        hasPrev = data.hasPrev
        hasNext = data.hasNext
        hasArrows = hasPrev || hasNext
        recalculateFontSize(pagedCandidates.size)
        notifyDataSetChanged()
    }

    fun updateLegacyCandidates(data: CandidateListEvent.Data) {
        candidates = data.candidates
        total = data.total
        activeIndex = -1
        isPagedMode = false
        hasArrows = false
        hasPrev = false
        hasNext = false
        indexOffset = 0
        recalculateFontSize(candidates.size)
        notifyDataSetChanged()
    }

    private fun recalculateFontSize(count: Int) {
        // Calculate font size based on available space
        // Fixed height area, each row takes ~itemHeightPx, max 2 rows
        val maxRows = 2
        val maxItemsPerRow = count.coerceAtMost(8) // rough max per row
        val rowsNeeded = (count + maxItemsPerRow - 1) / maxItemsPerRow
        // Scale font: more items = smaller font, capped at reasonable range
        val scale = when {
            rowsNeeded <= 1 -> 1.0f     // fits in 1 row
            rowsNeeded <= 2 -> 0.85f    // fits in 2 rows
            else -> 0.7f                // needs smaller font
        }
        val newSize = (fontSizeSp * scale).coerceIn(8f, 20f)
        currentFontSizeSp = newSize
    }

    fun getCurrentFontSize(): Float = currentFontSizeSp

    fun reset() {
        pagedCandidates = emptyArray()
        candidates = emptyArray()
        total = -1
        activeIndex = -1
        isPagedMode = false
        hasArrows = false
        hasPrev = false
        hasNext = false
        indexOffset = 0
        currentFontSizeSp = fontSizeSp
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        val base = if (isPagedMode) pagedCandidates.size else candidates.size
        return base + (if (hasArrows) 1 else 0)
    }

    override fun getItemId(position: Int): Long {
        val candidateCount = if (isPagedMode) pagedCandidates.size else candidates.size
        return if (hasArrows && position >= candidateCount) {
            Long.MAX_VALUE // fixed ID for pagination item
        } else if (isPagedMode) {
            pagedCandidates.getOrNull(position)?.hashCode()?.toLong() ?: position.toLong()
        } else {
            candidates.getOrNull(position)?.hashCode()?.toLong() ?: position.toLong()
        }
    }

    override fun getItemViewType(position: Int): Int {
        val candidateCount = if (isPagedMode) pagedCandidates.size else candidates.size
        return if (hasArrows && position >= candidateCount) VIEW_TYPE_PAGINATION
        else VIEW_TYPE_CANDIDATE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagedVcViewHolder {
        return if (viewType == VIEW_TYPE_PAGINATION) {
            createPaginationHolder(parent)
        } else {
            createCandidateHolder(parent)
        }
    }

    private fun createCandidateHolder(parent: ViewGroup): PagedVcViewHolder.Candidate {
        val context = parent.context
        val ui = PagedVerticalCandidateItemUi(context, theme)
        val itemHeight = if (itemHeightPx > 0) itemHeightPx else ViewGroup.LayoutParams.WRAP_CONTENT
        ui.root.layoutParams = FlexboxLayoutManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            itemHeight
        ).apply {
            flexGrow = 0f
            flexShrink = 0f
            flexBasisPercent = -1f
        }
        val holder = PagedVcViewHolder.Candidate(ui)
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onItemClick(pos)
            }
        }
        return holder
    }

    private fun createPaginationHolder(parent: ViewGroup): PagedVcViewHolder.Pagination {
        val context = parent.context
        val arrowW = context.dp(ARROW_WIDTH_DP)
        val arrowH = context.dp(ARROW_HEIGHT_DP)
        val disabledAlpha = context.styledFloat(android.R.attr.disabledAlpha)

        val prevIcon = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(arrowW, arrowH)
            imageTintList = ColorStateList.valueOf(theme.keyTextColor)
            imageDrawable = context.drawable(R.drawable.ic_baseline_arrow_prev_24)
            scaleType = ImageView.ScaleType.CENTER_CROP
            isClickable = true
            alpha = disabledAlpha
        }
        val nextIcon = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(arrowW, arrowH)
            imageTintList = ColorStateList.valueOf(theme.keyTextColor)
            imageDrawable = context.drawable(R.drawable.ic_baseline_arrow_next_24)
            scaleType = ImageView.ScaleType.CENTER_CROP
            isClickable = true
            alpha = disabledAlpha
        }

        val container = LinearLayout(context).apply {
            layoutParams = FlexboxLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                itemHeightPx
            ).apply {
                flexGrow = 0f
                flexShrink = 0f
                flexBasisPercent = -1f
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(prevIcon)
            addView(nextIcon)
        }

        return PagedVcViewHolder.Pagination(prevIcon, nextIcon, container)
    }

    override fun onBindViewHolder(holder: PagedVcViewHolder, position: Int) {
        when (holder) {
            is PagedVcViewHolder.Candidate -> bindCandidate(holder, position)
            is PagedVcViewHolder.Pagination -> bindPagination(holder)
        }
    }

    private fun bindCandidate(holder: PagedVcViewHolder.Candidate, position: Int) {
        holder.ui.text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp)

        if (isPagedMode) {
            val candidate = pagedCandidates.getOrNull(position)
            if (candidate != null) {
                holder.ui.update(candidate, position == activeIndex)
            }
        } else {
            val text = candidates.getOrNull(position) ?: ""
            val label = if (position < 9) "${position + 1}." else "${position + 1}."
            val candidate = FcitxEvent.Candidate(label = label, text = text, comment = "")
            holder.ui.update(candidate, (indexOffset + position) == activeIndex)
        }
    }

    private fun bindPagination(holder: PagedVcViewHolder.Pagination) {
        val context = holder.itemView.context
        val disabledAlpha = context.styledFloat(android.R.attr.disabledAlpha)

        // Set up page navigation clicks
        holder.prevIcon.setOnClickListener { onPrevPage() }
        holder.nextIcon.setOnClickListener { onNextPage() }

        // Update arrow alphas based on availability
        holder.prevIcon.alpha = if (hasPrev) 1f else disabledAlpha
        holder.nextIcon.alpha = if (hasNext) 1f else disabledAlpha
    }

    override fun onViewRecycled(holder: PagedVcViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PagedVcViewHolder.Pagination) {
            holder.prevIcon.setOnClickListener(null)
            holder.nextIcon.setOnClickListener(null)
        }
    }
}
