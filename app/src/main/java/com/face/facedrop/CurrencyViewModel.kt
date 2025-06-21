package com.face.facedrop

import android.content.SharedPreferences
import androidx.lifecycle.*
import com.google.gson.Gson
import kotlinx.coroutines.launch

class CurrencyViewModel(private val prefs: SharedPreferences) : ViewModel() {

    private val _rates = MutableLiveData<Map<String, Double>>()
    val rates: LiveData<Map<String, Double>> get() = _rates

    var baseCurrency: String = "USD"
    var baseValue: Double = 1.0

    private val apiKey = BuildConfig.API_KEY

    init {
        loadRatesFromPrefs() // Load cached data immediately on init
    }

    fun fetchRates(base: String, force: Boolean = false) {
        val lastFetchTime = prefs.getLong("last_fetch_time", 0L)
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000

        // Skip fetch if last fetch was within 24 hours and not forced
        if (!force && now - lastFetchTime < oneDayMillis) {
            println("📦 Using cached data (last fetch < 24h ago)")
            loadRatesFromPrefs()
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getRates(apiKey, base.uppercase())
                if (response.isSuccessful) {
                    val body = response.body()
                    val rawRates = body?.conversion_rates ?: emptyMap()
                    val normalizedRates = rawRates.mapKeys { it.key.uppercase() }

                    _rates.postValue(normalizedRates)
                    saveRatesToPrefs(normalizedRates, base.uppercase())

                    // Save last fetch time only AFTER successful fetch
                    prefs.edit().putLong("last_fetch_time", now).apply()

                    println("✅ Rates fetched and saved")
                } else {
                    println("❌ API error ${response.code()}: ${response.message()}")
                    loadRatesFromPrefs()
                }
            } catch (e: Exception) {
                println("❌ Exception while fetching rates: ${e.localizedMessage}")
                loadRatesFromPrefs()
            }
        }
    }



    fun updateBaseCurrency(newBase: String, newValue: Double) {
        baseCurrency = newBase.uppercase()
        baseValue = newValue
        fetchRates(baseCurrency)
    }

    private fun saveRatesToPrefs(rates: Map<String, Double>, base: String) {
        prefs.edit()
            .putString("saved_rates", Gson().toJson(rates))
            .putString("saved_base", base.uppercase())
            .apply()
    }

    private fun loadRatesFromPrefs() {
        val savedRatesJson = prefs.getString("saved_rates", null)
        val savedBase = prefs.getString("saved_base", "USD") ?: "USD"

        if (savedRatesJson != null) {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, Double>>() {}.type

            val savedRates: Map<String, Double> = Gson().fromJson<Map<String, Double>>(savedRatesJson, type)
                .mapKeys { entry: Map.Entry<String, Double> -> entry.key.uppercase() }


            _rates.postValue(savedRates)
            baseCurrency = savedBase.uppercase()
        } else {
            println("⚠️ No saved rates found.")
        }
    }
}
