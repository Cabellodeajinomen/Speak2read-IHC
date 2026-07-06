package com.example.speak2read

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.speak2read.database.Speak2ReadDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val prefsName = "s2r_settings"
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var database: Speak2ReadDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        database = Room.databaseBuilder(applicationContext, Speak2ReadDatabase::class.java, "speak2read_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        findViewById<TextView>(R.id.tvUserName).text = Speak2ReadPrefs.currentUserName(this)

        updateStatusLabels()

        // Font Size selection
        findViewById<LinearLayout>(R.id.btnFontSizeLayout).setOnClickListener {
            showFontSizeDialog()
        }

        // Theme selection
        findViewById<LinearLayout>(R.id.btnThemeLayout).setOnClickListener {
            showThemeDialog()
        }

        // Alarm detection (Switch makes sense here)
        val swAlarmDetection = findViewById<SwitchMaterial>(R.id.swAlarmDetection)
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        swAlarmDetection.isChecked = prefs.getBoolean("alarm_detection", true)
        
        swAlarmDetection.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("alarm_detection", isChecked).apply()
            if (isChecked) {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    startSoundService()
                } else {
                    androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
                }
            } else {
                stopSoundService()
            }
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
    }

    private fun startSoundService() {
        val intent = Intent(this, com.example.speak2read.service.SoundDetectionService::class.java)
        startForegroundService(intent)
    }

    private fun stopSoundService() {
        val intent = Intent(this, com.example.speak2read.service.SoundDetectionService::class.java)
        stopService(intent)
    }

    private fun updateStatusLabels() {
        val isLarge = Speak2ReadPrefs.isLargeFont(this)
        findViewById<TextView>(R.id.tvFontSizeStatus).text = if (isLarge) "Grande" else "Normal"
        
        val isDark = Speak2ReadPrefs.isDarkTheme(this)
        findViewById<TextView>(R.id.tvThemeStatus).text = if (isDark) "Oscuro" else "Claro"
    }

    private fun showFontSizeDialog() {
        val options = arrayOf("Normal", "Grande")
        val current = if (Speak2ReadPrefs.isLargeFont(this)) 1 else 0
        
        AlertDialog.Builder(this)
            .setTitle("Tamaño de fuente")
            .setSingleChoiceItems(options, current) { dialog, which ->
                val isLarge = which == 1
                getSharedPreferences(prefsName, MODE_PRIVATE).edit()
                    .putBoolean("font_size_large", isLarge).apply()
                dialog.dismiss()
                recreate()
            }
            .show()
    }

    private fun showThemeDialog() {
        val options = arrayOf("Claro", "Oscuro")
        val current = if (Speak2ReadPrefs.isDarkTheme(this)) 1 else 0
        
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Tema")
            .setSingleChoiceItems(options, current) { dialog, which ->
                val isDark = which == 1
                getSharedPreferences(prefsName, MODE_PRIVATE).edit()
                    .putBoolean("dark_theme", isDark).apply()
                dialog.dismiss()
                Speak2ReadPrefs.applySettings(this)
            }
            .show()
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
                val userId = auth.currentUser?.uid ?: "guest"
                database.messageDao().clear(userId)
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
                auth.signOut() // Real Firebase Logout
                Speak2ReadPrefs.clearSession(this)
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
