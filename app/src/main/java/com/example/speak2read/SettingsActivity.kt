package com.example.speak2read

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.room.Room
import com.example.speak2read.database.Speak2ReadDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private val prefsName = "s2r_settings"
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var database: Speak2ReadDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        database = Room.databaseBuilder(applicationContext, Speak2ReadDatabase::class.java, "speak2read_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        findViewById<TextView>(R.id.tvUserName).text = Speak2ReadPrefs.currentUserName(this)

        val swFontSize = findViewById<SwitchMaterial>(R.id.swFontSize)
        val swAlarmDetection = findViewById<SwitchMaterial>(R.id.swAlarmDetection)
        val swTheme = findViewById<SwitchMaterial>(R.id.swTheme)

        // Initialize state
        swFontSize.isChecked = prefs.getBoolean("font_size_large", true)
        swAlarmDetection.isChecked = prefs.getBoolean("alarm_detection", true)
        swTheme.isChecked = prefs.getBoolean("dark_theme", true)

        swFontSize.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("font_size_large", isChecked).apply()
            recreate() // Reiniciar para aplicar escala de fuente
        }

        swAlarmDetection.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alarm_detection", isChecked).apply()
        }

        swTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            Speak2ReadPrefs.applySettings(this) // Cambiar tema globalmente
        }

        // Professional layout buttons
        findViewById<LinearLayout>(R.id.btnCustomRepliesLayout).setOnClickListener {
            showCustomRepliesDialog()
        }

        findViewById<LinearLayout>(R.id.btnClearHistoryLayout).setOnClickListener {
            showClearHistoryDialog()
        }

        findViewById<LinearLayout>(R.id.btnLogoutLayout).setOnClickListener {
            showLogoutConfirmDialog()
        }

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_settings
        setupBottomNavigation()
        applyFontScale()
    }

    private fun applyFontScale() {
        val scale = Speak2ReadPrefs.fontScale(this)
        findViewById<TextView>(R.id.tvUserName).textSize = 20f * scale
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
        layout.setPadding(60, 20, 60, 20)

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
                Toast.makeText(this, "Respuestas guardadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_clear_history)
            .setMessage(R.string.settings_clear_history_confirm)
            .setPositiveButton("Borrar") { _, _ ->
                database.messageDao().clear()
                Toast.makeText(this, "Historial borrado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLogoutConfirmDialog() {
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
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }
}
