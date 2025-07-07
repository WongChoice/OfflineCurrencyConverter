package com.face.Firstcal.utils

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewSizer(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val itemsCount: Int = 6,        // Number of currency items expected
    private val heightPercent: Float = 0.5f // 50% of screen height by default
) {

    /**
     * Call this method once RecyclerView is initialized and data is ready.
     * It calculates the target height for RecyclerView and sets it.
     */
    fun adjustHeightToFitItems() {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val screenHeightPx = displayMetrics.heightPixels

        // Calculate desired RecyclerView height (e.g., 50% of screen height)
        val targetHeightPx = (screenHeightPx * heightPercent).toInt()

        // Optionally, calculate estimated item height based on targetHeightPx / itemsCount
        val itemHeightPx = targetHeightPx / itemsCount

        // Set fixed height on RecyclerView LayoutParams
        val params = recyclerView.layoutParams
        params.height = targetHeightPx
        recyclerView.layoutParams = params

        // Optional: Disable nested scrolling so RecyclerView won't scroll internally
        recyclerView.isNestedScrollingEnabled = false
    }
}
