package com.face.facedrop

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment

class CurrencySelectorDialog(
    private val allCurrencies: List<String>,
    private val selectedCurrencies: List<String>,
    private val onSelectionConfirmed: (List<String>) -> Unit
) : DialogFragment() {

    private val currentSelection = selectedCurrencies.toMutableSet()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val selectedArray = BooleanArray(allCurrencies.size) { i ->
            selectedCurrencies.contains(allCurrencies[i])
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Select up to 6 Currencies")
            .setMultiChoiceItems(allCurrencies.toTypedArray(), selectedArray) { _, index, isChecked ->
                val currency = allCurrencies[index]
                if (isChecked) {
                    if (currentSelection.size < 6) currentSelection.add(currency)
                    else Toast.makeText(requireContext(), "Max 6 currencies", Toast.LENGTH_SHORT).show()
                } else {
                    currentSelection.remove(currency)
                }
            }
            .setPositiveButton("OK") { _, _ ->
                onSelectionConfirmed(currentSelection.toList())
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
