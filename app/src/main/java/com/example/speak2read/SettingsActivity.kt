package com.example.speak2read

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Switch
import android.widget.Toast

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val swFontSize = findViewById<Switch>(R.id.swFontSize)
        val swAlarmDetection = findViewById<Switch>(R.id.swAlarmDetection)
        val swTheme = findViewById<Switch>(R.id.swTheme)
        val swLogout = findViewById<Switch>(R.id.swLogout)
        val btnViewProfile = findViewById<Button>(R.id.btnViewProfile)
        val tvProfileSummary = findViewById<TextView>(R.id.tvProfileSummary)

        swFontSize.isChecked = Speak2ReadPrefs.isLargeFont(this)
        swAlarmDetection.isChecked = Speak2ReadPrefs.isAlarmDetectionEnabled(this)
        swTheme.isChecked = Speak2ReadPrefs.isDarkTheme(this)
        swLogout.isChecked = false

        updateProfileSummary(tvProfileSummary)

        swFontSize.setOnCheckedChangeListener { _, isChecked ->
            Speak2ReadPrefs.settings(this).edit().putBoolean(Speak2ReadPrefs.KEY_FONT_SIZE_LARGE, isChecked).apply()
        }

        swAlarmDetection.setOnCheckedChangeListener { _, isChecked ->
            Speak2ReadPrefs.settings(this).edit().putBoolean(Speak2ReadPrefs.KEY_ALARM_DETECTION, isChecked).apply()
        }

        swTheme.setOnCheckedChangeListener { _, isChecked ->
            Speak2ReadPrefs.settings(this).edit().putBoolean(Speak2ReadPrefs.KEY_DARK_THEME, isChecked).apply()
        }

        btnViewProfile.setOnClickListener { showProfileDialog() }

        swLogout.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showLogoutDialog(swLogout)
            }
        }
    }

    private fun updateProfileSummary(summaryView: TextView) {
        if (!Speak2ReadPrefs.isLoggedIn(this)) {
            summaryView.text = getString(R.string.profile_not_logged)
            return
        }

        summaryView.text = getString(R.string.profile_summary, Speak2ReadPrefs.currentUserName(this))
    }

    private fun showProfileDialog() {
        val userName = Speak2ReadPrefs.currentUserName(this)
        val userType = Speak2ReadPrefs.currentUserType(this)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_title))
            .setMessage(getString(R.string.profile_summary, userName) + "\n" + getString(R.string.profile_type, userType))
            .setPositiveButton(getString(R.string.dialog_accept), null)
            .show()
    }

    private fun showLogoutDialog(logoutSwitch: Switch) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setPositiveButton(getString(R.string.dialog_accept)) { _, _ ->
                Speak2ReadPrefs.clearSession(this)
                val loginIntent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(loginIntent)
                Toast.makeText(this, getString(R.string.logout_cleared), Toast.LENGTH_SHORT).show()
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

