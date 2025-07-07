package com.face.Firstcal

import android.content.Context
import android.graphics.Color
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class CurrencyRowHelper(private val context: Context) {

    var focusedCurrency: String? = null
        private set

    fun setRowClickBehavior(
        rowLayout: LinearLayout,
        editText: EditText,
        currency: String,
        onFocusChanged: (String) -> Unit,
        onCurrencyReplaceRequested: (() -> Unit)? = null
    ) {
        rowLayout.setOnClickListener {
            if (focusedCurrency == currency) {
                // Second tap on already focused row: show replacement list
                onCurrencyReplaceRequested?.invoke()
            } else {
                // First tap: just focus
                setFocus(currency, editText, onFocusChanged)
            }
        }

        editText.setOnClickListener {
            if (focusedCurrency == currency) {
                onCurrencyReplaceRequested?.invoke()
            } else {
                setFocus(currency, editText, onFocusChanged)
            }
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setFocus(currency, editText, onFocusChanged)
            }
        }

        applyHighlight(rowLayout, editText, currency == focusedCurrency)
    }


    private fun setFocus(currency: String, editText: EditText, onFocusChanged: (String) -> Unit) {
        if (focusedCurrency != currency) {
            focusedCurrency = currency
            onFocusChanged(currency)
        }
        editText.requestFocus()
        editText.selectAll()
    }

    private fun applyHighlight(rowLayout: LinearLayout, editText: EditText, isFocused: Boolean) {
        if (isFocused) {
            rowLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
            editText.setTextColor(Color.BLACK)
        } else {
            rowLayout.setBackgroundColor(Color.TRANSPARENT)
            editText.setTextColor(Color.DKGRAY)
        }
    }
}
