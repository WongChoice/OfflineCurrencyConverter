package com.face.Firstcal

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.face.Firstcal.adapter.CurrencyAdapter
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: CurrencyViewModel
    private lateinit var adapter: CurrencyAdapter
    private val preferences by lazy { getSharedPreferences("currency_prefs", MODE_PRIVATE) }
    private val selectedCurrencies = mutableListOf<String>()
    private var allCurrencies: List<String> = emptyList()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuHelper: MenuHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Apply saved theme before super.onCreate
        applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        menuHelper = MenuHelper(this)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            val handled = menuHelper.onMenuItemSelected(menuItem.itemId)
            if (handled) drawerLayout.closeDrawer(GravityCompat.START)
            handled
        }

        val factory = CurrencyViewModelFactory(preferences)
        viewModel = ViewModelProvider(this, factory)[CurrencyViewModel::class.java]

        val saved = preferences.getStringSet("selected_currencies", null)
        if (saved != null && saved.isNotEmpty()) {
            selectedCurrencies.addAll(saved)
        } else {
            selectedCurrencies.addAll(listOf("USD", "EUR", "INR", "JPY", "GBP", "CAD"))
        }

        val savedBaseCurrency = preferences.getString("saved_base", "USD") ?: "USD"
        if (selectedCurrencies.isNotEmpty()) {
            if (selectedCurrencies[0] != savedBaseCurrency) {
                if (selectedCurrencies.contains(savedBaseCurrency)) {
                    selectedCurrencies.remove(savedBaseCurrency)
                }
                selectedCurrencies.add(0, savedBaseCurrency)
            }
            viewModel.baseCurrency = selectedCurrencies[0]
            preferences.edit {
                putStringSet("selected_currencies", selectedCurrencies.toSet())
                putString("saved_base", selectedCurrencies[0])
            }
        } else {
            selectedCurrencies.add(savedBaseCurrency)
        }

        preferences.edit {
            putStringSet("selected_currencies", selectedCurrencies.toSet())
        }

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

        val recyclerView = findViewById<RecyclerView>(R.id.currencyList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CurrencyAdapter(
            selectedCurrencies,
            onBaseCurrencyChanged = { newBaseCurrency, newBaseValue ->
                viewModel.updateBaseCurrency(newBaseCurrency, newBaseValue)
            },
            onCurrencyReplaceRequested = { oldCurrency, position ->
                showCurrencyReplaceDialog(oldCurrency, position)
            }
        )

        recyclerView.adapter = adapter

        viewModel.rates.observe(this) { rates ->
            allCurrencies = rates.keys.sorted()
            updateLastUpdatedMessage()

            if (!selectedCurrencies.contains(viewModel.baseCurrency)) {
                Log.w("MainActivity", "Base currency not in selectedCurrencies, but no forced fix applied.")
            }
            adapter.updateValues(viewModel.baseCurrency, viewModel.baseValue, rates)
        }

        viewModel.baseValue = 1.0

        lifecycleScope.launch {
            try {
                viewModel.fetchRates(viewModel.baseCurrency)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to fetch rates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun applySavedTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun appendDigit(digit: String) {
        val currency = adapter.focusedCurrency
        if (currency != null) {
            adapter.appendDigitToCurrencyAmount(currency, digit)
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteLastDigit() {
        val currency = adapter.focusedCurrency
        if (currency != null) {
            adapter.deleteLastDigitFromCurrencyAmount(currency)
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAmount() {
        val currency = adapter.focusedCurrency
        if (currency != null) {
            adapter.clearCurrencyAmount(currency)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showCurrencyReplaceDialog(oldCurrency: String, position: Int) {
        CurrencySelectionDialog(
            this,
            allCurrencies,
            selectedCurrencies,
            oldCurrency,
            position
        ) { newCurrency, pos ->
            selectedCurrencies[pos] = newCurrency
            adapter.updateCurrencies(selectedCurrencies)
            preferences.edit {
                putStringSet("selected_currencies", selectedCurrencies.toSet())
            }
            if (pos == 0) {
                viewModel.baseCurrency = newCurrency
                preferences.edit {
                    putString("saved_base", newCurrency)
                }
            }
        }.show()
    }

    internal fun showCurrencySelector() {
        if (allCurrencies.isEmpty()) {
            Toast.makeText(this, "Currency list not loaded yet. Try again shortly.", Toast.LENGTH_SHORT).show()
            return
        }
        CurrencySelectorDialog(allCurrencies, selectedCurrencies) { newSelection ->
            if (newSelection.isEmpty()) {
                Toast.makeText(this, "Please select at least one currency", Toast.LENGTH_SHORT).show()
                return@CurrencySelectorDialog
            }
            val newSelectedCurrencies = newSelection.toMutableList()
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                val rect = android.graphics.Rect()
                view.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                    view.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
