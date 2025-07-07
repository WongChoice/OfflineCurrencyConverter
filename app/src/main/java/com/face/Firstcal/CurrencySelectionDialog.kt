package com.face.Firstcal

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.face.Firstcal.databinding.DialogCurrencySelectionBinding
import com.face.Firstcal.databinding.ItemCurrencyDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class CurrencySelectionDialog(
    private val context: Context,
    private val currencyList: List<String>,
    private val selectedCurrencies: List<String>,
    private val oldCurrency: String,
    private val position: Int,
    private val onCurrencySelected: (newCurrency: String, position: Int) -> Unit
) {
    private var dialog: BottomSheetDialog? = null

    fun show() {
        val filteredCurrencies = currencyList.filter { it !in selectedCurrencies || it == oldCurrency }

        val binding = DialogCurrencySelectionBinding.inflate(LayoutInflater.from(context))

        val recyclerView = binding.recyclerCurrencies
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = CurrencyListAdapter(filteredCurrencies) { selectedCurrency ->
            onCurrencySelected(selectedCurrency, position)
            dialog?.dismiss()
        }

        dialog = BottomSheetDialog(context)
        dialog?.setContentView(binding.root)

        // Optional: expand to full screen height
        dialog?.behavior?.apply {
            isFitToContents = true
            expandedOffset = 0
        }

        dialog?.show()
    }

    private class CurrencyListAdapter(
        private val currencies: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<CurrencyListAdapter.CurrencyViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemCurrencyDialogBinding.inflate(inflater, parent, false)
            return CurrencyViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
            val currency = currencies[position]
            holder.bind(currency, onItemClick)
        }

        override fun getItemCount(): Int = currencies.size

        inner class CurrencyViewHolder(private val binding: ItemCurrencyDialogBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(currency: String, onClick: (String) -> Unit) {
                binding.currencyCode.text = currency
                binding.root.setOnClickListener { onClick(currency) }
            }
        }
    }
}
