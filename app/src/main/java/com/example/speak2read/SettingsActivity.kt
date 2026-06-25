package com.example.speak2read

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : Activity() {

    private val prefsName = "s2r_settings"
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)

        val swFontSize = findViewById<Switch>(R.id.swFontSize)
        val swAlarmDetection = findViewById<Switch>(R.id.swAlarmDetection)
        val swTheme = findViewById<Switch>(R.id.swTheme)
        val swLogout = findViewById<Switch>(R.id.swLogout)

        swFontSize.isChecked = prefs.getBoolean("font_size_large", true)
        swAlarmDetection.isChecked = prefs.getBoolean("alarm_detection", true)
        swTheme.isChecked = prefs.getBoolean("dark_theme", true)
        swLogout.isChecked = false

        swFontSize.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("font_size_large", isChecked).apply()
        }

        swAlarmDetection.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alarm_detection", isChecked).apply()
        }

        swTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
        }

        swLogout.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showLogoutDialog(swLogout)
            }
        }

        findViewById<Button>(R.id.btnCustomReplies).setOnClickListener {
            showCustomRepliesDialog()
        }

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_settings
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_conversations -> {
                    startActivity(Intent(this, ConversationsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun showCustomRepliesDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val custom = Speak2ReadPrefs.getCustomReplies(this)

        val et1 = EditText(this).apply { setText(custom[0]); hint = "Respuesta 1" }
        val et2 = EditText(this).apply { setText(custom[1]); hint = "Respuesta 2" }
        val et3 = EditText(this).apply { setText(custom[2]); hint = "Respuesta 3" }
        val et4 = EditText(this).apply { setText(custom[3]); hint = "Respuesta 4" }

        layout.addView(et1)
        layout.addView(et2)
        layout.addView(et3)
        layout.addView(et4)

        AlertDialog.Builder(this)
            .setTitle("Respuestas personalizadas")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                Speak2ReadPrefs.saveCustomReplies(
                    this,
                    et1.text.toString(),
                    et2.text.toString(),
                    et3.text.toString(),
                    et4.text.toString()
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLogoutDialog(logoutSwitch: Switch) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setPositiveButton(getString(R.string.dialog_accept)) { _, _ ->
                val loginIntent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(loginIntent)
                finish()
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { _, _ ->
                logoutSwitch.isChecked = false
            }
            .setOnCancelListener {
                logoutSwitch.isChecked = false
            }
            .show()
    }
}
