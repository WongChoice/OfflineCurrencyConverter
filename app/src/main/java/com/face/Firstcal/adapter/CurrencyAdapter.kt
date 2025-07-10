package com.face.Firstcal.adapter

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.face.Firstcal.CurrencyRowHelper
import com.face.Firstcal.databinding.ItemCurrencyBinding

class CurrencyAdapter(
    private var currencies: MutableList<String>,
    private val onBaseCurrencyChanged: (String, Double) -> Unit,
    private val onCurrencyReplaceRequested: (oldCurrency: String, position: Int) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    private val valuesString = mutableMapOf<String, String>()
    private val values = mutableMapOf<String, Double>()
    private var rates: Map<String, Double> = emptyMap()

    private var baseCurrencyInternal: String = "USD"
    private var baseValueInternal: Double = 1.0

    var focusedCurrency: String? = null
        private set

    private lateinit var rowHelper: CurrencyRowHelper
    private var recyclerViewRef: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViewRef = recyclerView
        rowHelper = CurrencyRowHelper(recyclerView.context)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerViewRef = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currency = currencies[position]
        val rowLayout = holder.binding.root as LinearLayout
        val editText = holder.binding.currencyValue

        holder.bind(currency)

        rowHelper.setRowClickBehavior(rowLayout, editText, currency, { newFocusedCurrency ->
            if (focusedCurrency != newFocusedCurrency) {
                focusedCurrency = newFocusedCurrency
                notifyDataSetChanged()
                recyclerViewRef?.scrollToPosition(position)
            }
        }, onCurrencyReplaceRequested = {
            this.onCurrencyReplaceRequested(currency, position)
        })
    }

    override fun getItemCount(): Int = currencies.size

    fun updateCurrencies(newCurrencies: List<String>) {
        currencies = newCurrencies.toMutableList()
        // After updating currencies, auto-update values using stored base and rates
        updateValues(baseCurrencyInternal, baseValueInternal, rates)
    }

    fun updateValues(baseCurrency: String, baseValue: Double, rates: Map<String, Double>) {
        this.baseCurrencyInternal = baseCurrency
        this.baseValueInternal = baseValue
        this.rates = rates

        currencies.forEach { currency ->
            val inputToUsd = 1.0 / (rates[baseCurrency] ?: 1.0)
            val usdToTarget = rates[currency] ?: 1.0

            val rate = inputToUsd * usdToTarget
            val newValue = baseValue * rate

            values[currency] = newValue

            if (currency != focusedCurrency) {
                valuesString[currency] = String.format("%.2f", newValue)
            }
        }

        notifyDataSetChanged()
    }

    fun appendDigitToCurrencyAmount(currency: String, digit: String) {
        if (focusedCurrency != currency) {
            Log.d("CurrencyAdapter", "Ignored append; $currency is not focused")
            return
        }

        val currentText = valuesString[currency] ?: "0"

        val newText = when {
            digit == "." && currentText.contains(".") -> currentText
            currentText == "0" && digit != "." -> digit
            currentText == "0" && digit == "." -> "0."
            else -> currentText + digit
        }

        updateCurrencyAmount(currency, newText)
    }

    fun deleteLastDigitFromCurrencyAmount(currency: String) {
        if (focusedCurrency != currency) {
            Log.d("CurrencyAdapter", "Ignored delete; $currency is not focused")
            return
        }

        val currentText = valuesString[currency] ?: "0"
        val newText = if (currentText.length <= 1) "0" else currentText.dropLast(1)
        updateCurrencyAmount(currency, newText)
    }
    fun clearCurrencyAmount(currency: String) {
        if (focusedCurrency != currency) {
            Log.d("CurrencyAdapter", "Ignored clear; $currency is not focused")
            return
        }

        updateCurrencyAmount(currency, "0")
    }

    private fun updateCurrencyAmount(currency: String, newText: String) {
        valuesString[currency] = newText
        val newValue = newText.toDoubleOrNull() ?: 0.0
        values[currency] = newValue

        if (focusedCurrency == currency) {
            onBaseCurrencyChanged(currency, newValue)
        }
    }

    inner class CurrencyViewHolder(internal val binding: ItemCurrencyBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWatcher: TextWatcher? = null

        fun bind(currency: String) {
            binding.currencyCode.text = currency
            binding.root.setOnClickListener {
                onCurrencyReplaceRequested(currency, bindingAdapterPosition)
            }

            currentWatcher?.let { binding.currencyValue.removeTextChangedListener(it) }

            if (currency == focusedCurrency) {
                val rawText = valuesString[currency] ?: "0"
                if (binding.currencyValue.text.toString() != rawText) {
                    binding.currencyValue.setText(rawText)
                    binding.currencyValue.setSelection(rawText.length)
                }
            } else {
                val formatted = valuesString[currency] ?: "0.00"
                if (binding.currencyValue.text.toString() != formatted) {
                    binding.currencyValue.setText(formatted)
                }
            }

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (focusedCurrency != currency) return

                    val input = s.toString()
                    if (!isValidInput(input)) {
                        binding.currencyValue.removeTextChangedListener(this)
                        binding.currencyValue.setText(valuesString[currency])
                        binding.currencyValue.setSelection(valuesString[currency]?.length ?: 0)
                        binding.currencyValue.addTextChangedListener(this)
                        return
                    }

                    updateCurrencyAmount(currency, input)
                }
            }

            binding.currencyValue.addTextChangedListener(watcher)
            currentWatcher = watcher

            binding.currencyValue.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusedCurrency = currency
                    Log.d("CurrencyAdapter", "Focused currency set to $currency")
                    binding.currencyValue.post { binding.currencyValue.selectAll() }
                } else if (focusedCurrency == currency) {
                    val input = binding.currencyValue.text.toString()
                    updateCurrencyAmount(currency, input)
                    focusedCurrency = null
                }
            }

            binding.currencyValue.setOnClickListener {
                focusedCurrency = currency
                Log.d("CurrencyAdapter", "Focused currency set to $currency via click")
            }
        }
    }

    private fun isValidInput(input: String): Boolean {
        val regex = Regex("^\\d*(\\.\\d{0,2})?$")
        if (!regex.matches(input)) return false
        if (input.startsWith("0") && input.length > 1 && !input.startsWith("0.")) return false
        return true
    }
}
