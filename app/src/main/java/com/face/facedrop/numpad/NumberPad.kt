package com.face.facedrop.numpad

import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.lifecycle.MutableLiveData
import com.face.facedrop.R

class NumberPad(
    rootView: View,
    private val valueLiveData: MutableLiveData<Double>
) {
    private var currentText = ""

    init {
        val grid = rootView.findViewById<GridLayout>(R.id.numberPad)
        for (i in 0 until grid.childCount) {
            val button = grid.getChildAt(i) as Button
            button.setOnClickListener { onButtonClicked(button.text.toString()) }
        }
    }

    private fun onButtonClicked(text: String) {
        when (text) {
            "⌫" -> {
                if (currentText.isNotEmpty()) {
                    currentText = currentText.dropLast(1)
                }
            }
            "." -> {
                if (!currentText.contains(".")) {
                    currentText += "."
                }
            }
            else -> currentText += text
        }

        val value = currentText.toDoubleOrNull() ?: 0.0
        valueLiveData.value = value
    }
}
