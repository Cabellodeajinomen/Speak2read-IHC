package com.example.speak2read

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.widget.ImageButton
import android.widget.TextView

class FullscreenTranscriptionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_transcription)

        val tv = findViewById<TextView>(R.id.tvFullscreen)
        val btnClose = findViewById<ImageButton>(R.id.btnCloseFullscreen)
        
        val text = intent?.getStringExtra("text") ?: ""
        tv.text = text
        
        // Letra realmente grande para IHC
        tv.textSize = 42f * Speak2ReadPrefs.fontScale(this)
        
        val background = if (Speak2ReadPrefs.isDarkTheme(this)) Color.parseColor("#1B1D22") else Color.parseColor("#F5F5F5")
        val textColor = if (Speak2ReadPrefs.isDarkTheme(this)) Color.WHITE else Color.parseColor("#1B1D22")
        
        findViewById<android.view.View>(android.R.id.content).setBackgroundColor(background)
        tv.setTextColor(textColor)
        
        btnClose.setOnClickListener {
            finish()
        }
    }
}
