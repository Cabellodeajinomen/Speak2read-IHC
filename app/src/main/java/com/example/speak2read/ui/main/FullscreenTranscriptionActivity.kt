package com.example.speak2read.ui.main

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.speak2read.R
import com.example.speak2read.data.local.Speak2ReadPrefs

class FullscreenTranscriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_transcription)

        val text = intent.getStringExtra("text") ?: ""
        val tvContent = findViewById<TextView>(R.id.tvFullscreen)
        tvContent.text = text

        val scale = Speak2ReadPrefs.fontScale(this)
        tvContent.textSize = 42f * scale // Modo Zoom para IHC

        findViewById<ImageButton>(R.id.btnCloseFullscreen).setOnClickListener {
            finish()
        }
    }
}
