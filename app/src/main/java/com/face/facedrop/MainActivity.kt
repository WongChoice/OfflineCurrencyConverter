package com.face.facedrop

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.face.facedrop.adapter.CurrencyAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: CurrencyViewModel
    private lateinit var adapter: CurrencyAdapter
    private val preferences by lazy { getSharedPreferences("currency_prefs", MODE_PRIVATE) }
    private val selectedCurrencies = mutableListOf<String>()
    private var allCurrencies: List<String> = emptyList() // loaded dynamically

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val factory = CurrencyViewModelFactory(preferences)
        viewModel = ViewModelProvider(this, factory)[CurrencyViewModel::class.java]

        // Load saved selected currencies or default list
        val saved = preferences.getStringSet("selected_currencies", null)

        val lastUpdatedText = findViewById<TextView>(R.id.lastUpdatedText)

        val lastFetchMillis = preferences.getLong("last_fetch_time", 0L)
        if (lastFetchMillis > 0) {
            val now = System.currentTimeMillis()
            val diff = now - lastFetchMillis

            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff / (1000 * 60)) % 60

            lastUpdatedText.text = if (diff < 24 * 60 * 60 * 1000) {
                "✅ Last updated: ${hours}h ${minutes}m ago"
            } else {
                "⚠️ Using old data: Last updated over 1 day ago"
            }
        } else {
            lastUpdatedText.text = "⚠️ No saved data available"
        }

        if (saved != null && saved.isNotEmpty()) {
            selectedCurrencies.addAll(saved)
        } else {
            selectedCurrencies.addAll(listOf("USD", "EUR", "INR", "JPY", "GBP", "CAD"))
        }

        val recyclerView = findViewById<RecyclerView>(R.id.currencyList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CurrencyAdapter(selectedCurrencies,
            onBaseCurrencyChanged = { newBaseCurrency, newBaseValue ->
                viewModel.updateBaseCurrency(newBaseCurrency, newBaseValue)
            },
            onCurrencyReplaceRequested = { oldCurrency, position ->
                showCurrencyReplaceDialog(oldCurrency, position)
            }
        )

        recyclerView.adapter = adapter

        // Observe currency rates and update adapter
        viewModel.rates.observe(this) { rates ->
            allCurrencies = rates.keys.sorted()
            updateLastUpdatedMessage()

            // Fix: Avoid duplicates when adding baseCurrency
            // Remove forced insert or replacement of baseCurrency here
// Instead just trust selectedCurrencies as-is, so user changes persist

// Optionally you can log if baseCurrency missing, but do not forcibly fix
            if (!selectedCurrencies.contains(viewModel.baseCurrency)) {
                Log.w("MainActivity", "Base currency not in selectedCurrencies, but no forced fix applied.")
            }

            adapter.updateValues(viewModel.baseCurrency, viewModel.baseValue, rates)
        }

        // Ensure cached baseCurrency is in the list (add if needed)
        if (!selectedCurrencies.contains(viewModel.baseCurrency)) {
            if (selectedCurrencies.isNotEmpty()) {
                selectedCurrencies[0] = viewModel.baseCurrency
            } else {
                selectedCurrencies.add(viewModel.baseCurrency)
            }
        }

        // No need to override baseCurrency now
        viewModel.baseValue = 1.0

        // Initial fetch of rates
        lifecycleScope.launch {
            try {
                viewModel.fetchRates(viewModel.baseCurrency)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to fetch rates: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Setup keypad button listeners
        findViewById<Button>(R.id.btn0).setOnClickListener { appendDigit("0") }
        findViewById<Button>(R.id.btn1).setOnClickListener { appendDigit("1") }
        findViewById<Button>(R.id.btn2).setOnClickListener { appendDigit("2") }
        findViewById<Button>(R.id.btn3).setOnClickListener { appendDigit("3") }
        findViewById<Button>(R.id.btn4).setOnClickListener { appendDigit("4") }
        findViewById<Button>(R.id.btn5).setOnClickListener { appendDigit("5") }
        findViewById<Button>(R.id.btn6).setOnClickListener { appendDigit("6") }
        findViewById<Button>(R.id.btn7).setOnClickListener { appendDigit("7") }
        findViewById<Button>(R.id.btn8).setOnClickListener { appendDigit("8") }
        findViewById<Button>(R.id.btn9).setOnClickListener { appendDigit("9") }
        findViewById<Button>(R.id.btnDot).setOnClickListener { appendDigit(".") }

        findViewById<Button>(R.id.btnDelete).setOnClickListener { deleteLastDigit() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAmount() }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun appendDigit(digit: String) {
        val currency = adapter.focusedCurrency
        Log.d("MainActivity", "Appending digit '$digit' to focusedCurrency=$currency")
        if (currency != null) {
            adapter.appendDigitToCurrencyAmount(currency, digit)
            adapter.notifyDataSetChanged() // Force update all currencies
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteLastDigit() {
        val currency = adapter.focusedCurrency
        if (currency != null) {
            adapter.deleteLastDigitFromCurrencyAmount(currency)
            adapter.notifyDataSetChanged()
        } else {
            Log.d("MainActivity", "deleteLastDigit: No focused currency")
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAmount() {
        val currency = adapter.focusedCurrency
        if (currency != null) {
            adapter.clearCurrencyAmount(currency)
            adapter.notifyDataSetChanged()
        } else {
            Log.d("MainActivity", "clearAmount: No focused currency")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_select_currency -> {
                showCurrencySelector()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCurrencyReplaceDialog(oldCurrency: String, position: Int) {
        val available = allCurrencies.filter { it !in selectedCurrencies || it == oldCurrency }
        val items = available.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Replace $oldCurrency with...")
            .setItems(items) { _, which ->
                val newCurrency = items[which]
                selectedCurrencies[position] = newCurrency
                adapter.updateCurrencies(selectedCurrencies)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCurrencySelector() {
        if (allCurrencies.isEmpty()) {
            Toast.makeText(this, "Currency list not loaded yet. Try again shortly.", Toast.LENGTH_SHORT).show()
            return
        }

        CurrencySelectorDialog(
            allCurrencies,
            selectedCurrencies
        ) { newSelection ->
            if (newSelection.isEmpty()) {
                Toast.makeText(this, "Please select at least one currency", Toast.LENGTH_SHORT).show()
                return@CurrencySelectorDialog
            }

            val newSelectedCurrencies = newSelection.toMutableList()

            // If user removed baseCurrency, reset baseCurrency to first selected
            if (!newSelectedCurrencies.contains(viewModel.baseCurrency)) {
                viewModel.baseCurrency = newSelectedCurrencies.first()
                viewModel.baseValue = 1.0
            }

            selectedCurrencies.clear()
            selectedCurrencies.addAll(newSelectedCurrencies)
            preferences.edit {
                putStringSet("selected_currencies", selectedCurrencies.toSet())
            }

            adapter.updateCurrencies(selectedCurrencies)

            lifecycleScope.launch {
                try {
                    viewModel.fetchRates(viewModel.baseCurrency)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Failed to fetch rates: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.show(supportFragmentManager, "currency_selector")
    }

    private fun updateLastUpdatedMessage() {
        val lastUpdatedText = findViewById<TextView>(R.id.lastUpdatedText)
        val lastFetchMillis = preferences.getLong("last_fetch_time", 0L)
        if (lastFetchMillis == 0L) {
            lastUpdatedText.text = "⚠️ No saved data available"
            return
        }

        val now = System.currentTimeMillis()
        val diff = now - lastFetchMillis
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60

        lastUpdatedText.text = if (diff < 24 * 60 * 60 * 1000) {
            "✅ Last updated: ${hours}h ${minutes}m ago"
        } else {
            "⚠️ Using old data: Last updated over 1 day ago"
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                val rect = android.graphics.Rect()
                view.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    // User tapped outside the focused EditText
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    view.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
