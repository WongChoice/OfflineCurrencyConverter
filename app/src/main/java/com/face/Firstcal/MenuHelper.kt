package com.face.Firstcal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.content.res.Configuration
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate

class MenuHelper(private val context: Context) {

    fun onMenuItemSelected(itemId: Int): Boolean {
        return when (itemId) {
            R.id.menu_select_currency -> {
                if (context is MainActivity) {
                    context.showCurrencySelector()
                }
                true
            }
            R.id.action_support_us -> {
                openSupportPage()
                true
            }
            R.id.action_source_code -> {
                openUrl("https://github.com/yourusername/your-repo")
                true
            }
            R.id.action_preferences -> {
                showPreferences()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            R.id.action_toggle_theme -> {
                toggleTheme()
                true
            }
            else -> false
        }
    }

    private fun openSupportPage() {
        val url = "https://www.patreon.com/yourpage"
        openUrl(url)
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private fun showPreferences() {
        Toast.makeText(context, "Preferences clicked", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(context)
            .setTitle("About This App")
            .setMessage("This app is 100% open source and ad-free.\n\nPlease consider supporting us to help keep it growing!")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleTheme() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            prefs.edit().putBoolean("dark_mode", false).apply()
            Toast.makeText(context, "Switched to Light Mode", Toast.LENGTH_SHORT).show()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            prefs.edit().putBoolean("dark_mode", true).apply()
            Toast.makeText(context, "Switched to Dark Mode", Toast.LENGTH_SHORT).show()
        }

        if (context is MainActivity) {
            context.recreate()
        }
    }
}
