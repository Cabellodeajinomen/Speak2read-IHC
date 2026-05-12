package com.example.speak2read

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.graphics.Color
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.speak2read.adapter.ChatAdapter
import com.example.speak2read.model.ChatMessage
import com.example.speak2read.model.MessageType
import org.json.JSONArray
import java.util.Locale

class HomeActivity : Activity(), TextToSpeech.OnInitListener {

    private val defaultQuickReplies = listOf(
        "Sí, claro",
        "No entiendo",
        "Repita por favor",
        "Hable más despacio",
        "¿Me puede escribir eso?",
        "Gracias",
        "Un momento",
        "Estoy listo/a"
    )
    private val quickReplyPrefs by lazy { getSharedPreferences("s2r_quick_replies", MODE_PRIVATE) }

    private lateinit var rvChat: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var etMessage: EditText
    private lateinit var etTranscription: EditText
    private lateinit var btnSpeaker: ImageButton
    private lateinit var btnMicTranscription: ImageButton
    private lateinit var btnFullscreenTranscription: ImageButton
    private lateinit var btnSendTranscription: ImageButton
    private lateinit var btnSendMessage: ImageButton
    private lateinit var btnAcknowledge: Button
    private lateinit var emergencyOverlay: View
    private lateinit var imgWarning: ImageView
    private lateinit var quickReplyContainer: LinearLayout
    private lateinit var mainContainer: View

    private var listening = false
    private var tts: TextToSpeech? = null
    private var micPulse: ObjectAnimator? = null

    private val emergencyAction = "com.example.speak2read.ACTION_EMERGENCY"
    private var receiverRegistered = false

    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Mostrar overlay
            emergencyOverlay.visibility = View.VISIBLE
            startWarningBlink()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tts = TextToSpeech(this, this)

        rvChat = findViewById(R.id.recyclerChat)
        etMessage = findViewById(R.id.etMessage)
        etTranscription = findViewById(R.id.etTranscription)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        btnMicTranscription = findViewById(R.id.btnMicTranscription)
        btnFullscreenTranscription = findViewById(R.id.btnFullscreenTranscription)
        btnSendTranscription = findViewById(R.id.btnSendTranscription)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        emergencyOverlay = findViewById(R.id.emergencyOverlay)
        btnAcknowledge = findViewById(R.id.btnAcknowledge)
        imgWarning = findViewById(R.id.imgWarning)
        quickReplyContainer = findViewById(R.id.quickReplyContainer)
        mainContainer = findViewById(R.id.mainContainer)

        applyThemeAndFont()

        adapter = ChatAdapter(
            onExpandMessage = { message -> showExpandedMessage(message) },
            onSpeakMessage = { message -> speakText(message) },
            fontScale = Speak2ReadPrefs.fontScale(this)
        )
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        renderQuickReplies()

        // Settings
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Mic único para STT (simulado)
        btnMicTranscription.setOnClickListener {
            listening = !listening
            if (listening) startMicPulse() else stopMicPulse()
        }

        // Mic transcription button (simulado): agrega texto a transcription
        btnMicTranscription.setOnLongClickListener {
            etTranscription.setText(getString(R.string.home_transcription_sample))
            true
        }

        // Fullscreen transcription: abrir nueva Activity simple que muestra texto en grande
        btnFullscreenTranscription.setOnClickListener {
            val intent = Intent(this, FullscreenTranscriptionActivity::class.java)
            intent.putExtra("text", etTranscription.text.toString())
            startActivity(intent)
        }

        btnSendTranscription.setOnClickListener {
            val text = etTranscription.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, MessageType.RECEIVE)
                etTranscription.text?.clear()
            }
        }

        // Speaker (TTS)
        btnSpeaker.setOnClickListener {
            val text = etMessage.text.toString()
            speakText(text)
        }

        btnSendMessage.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, MessageType.SEND)
                etMessage.text?.clear()
            }
        }

        // Overlay acknowledge
        btnAcknowledge.setOnClickListener {
            emergencyOverlay.visibility = View.GONE
            stopWarningBlink()
        }

    }

    override fun onStart() {
        super.onStart()
        registerEmergencyReceiverIfNeeded()
    }

    override fun onStop() {
        unregisterEmergencyReceiverIfNeeded()
        super.onStop()
    }

    private fun insertQuickReply(text: String) {
        etMessage.setText(text)
    }

    private fun applyThemeAndFont() {
        val largeFont = Speak2ReadPrefs.fontScale(this)
        etMessage.textSize = 20f * largeFont
        etTranscription.textSize = 20f * largeFont
        btnSpeaker.scaleX = 1f
        btnSpeaker.scaleY = 1f

        val background = if (Speak2ReadPrefs.isDarkTheme(this)) Color.parseColor("#121212") else Color.parseColor("#F5F5F5")
        val textColor = if (Speak2ReadPrefs.isDarkTheme(this)) Color.WHITE else Color.parseColor("#121212")
        mainContainer.setBackgroundColor(background)
        rvChat.setBackgroundColor(background)
        etMessage.setTextColor(textColor)
        etMessage.setHintTextColor(textColor)
        etTranscription.setTextColor(textColor)
        etTranscription.setHintTextColor(textColor)
    }

    private fun renderQuickReplies() {
        quickReplyContainer.removeAllViews()

        getQuickReplies().forEach { reply ->
            quickReplyContainer.addView(createQuickReplyButton(reply))
        }

        quickReplyContainer.addView(createManageQuickRepliesButton())
    }

    private fun getQuickReplies(): MutableList<String> {
        val raw = quickReplyPrefs.getString("items", null) ?: return defaultQuickReplies.toMutableList()
        val parsed = mutableListOf<String>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                array.optString(i).trim().takeIf { it.isNotEmpty() }?.let(parsed::add)
            }
        } catch (_: Exception) {
            return defaultQuickReplies.toMutableList()
        }
        return if (parsed.isEmpty()) defaultQuickReplies.toMutableList() else parsed
    }

    private fun saveQuickReplies(replies: List<String>) {
        quickReplyPrefs.edit().putString("items", JSONArray(replies).toString()).apply()
    }

    private fun createQuickReplyButton(text: String): Button {
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 18f * Speak2ReadPrefs.fontScale(context)
            setTextColor(if (Speak2ReadPrefs.isDarkTheme(context)) Color.WHITE else Color.BLACK)
            setBackgroundResource(R.drawable.bg_quick_reply_card)
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 16
            }
            setOnClickListener { insertQuickReply(text) }
            setOnLongClickListener {
                speakText(text)
                true
            }
        }
    }

    private fun createManageQuickRepliesButton(): Button {
        return Button(this).apply {
            text = getString(R.string.quick_reply_manage)
            isAllCaps = false
            textSize = 18f * Speak2ReadPrefs.fontScale(context)
            setTextColor(if (Speak2ReadPrefs.isDarkTheme(context)) Color.WHITE else Color.BLACK)
            setBackgroundResource(R.drawable.bg_quick_reply_card)
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { showQuickRepliesManager() }
        }
    }

    private fun showQuickRepliesManager() {
        val options = arrayOf(
            getString(R.string.quick_reply_add_custom),
            getString(R.string.quick_reply_reorder),
            getString(R.string.quick_reply_restore_defaults)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.quick_reply_dialog_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> promptAddCustomQuickReply()
                    1 -> promptReorderQuickReplies()
                    2 -> {
                        saveQuickReplies(defaultQuickReplies)
                        renderQuickReplies()
                    }
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun promptAddCustomQuickReply() {
        val input = EditText(this).apply {
            hint = getString(R.string.quick_reply_custom_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.quick_reply_add_custom))
            .setView(input)
            .setPositiveButton(getString(R.string.dialog_accept)) { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isEmpty()) return@setPositiveButton

                val current = getQuickReplies()
                if (current.any { it.equals(newText, ignoreCase = true) }) {
                    Toast.makeText(this, getString(R.string.quick_reply_duplicate), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                current.add(newText)
                saveQuickReplies(current)
                renderQuickReplies()
                Toast.makeText(this, getString(R.string.quick_reply_added), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun promptReorderQuickReplies() {
        val current = getQuickReplies()
        if (current.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.quick_reply_reorder))
            .setItems(current.toTypedArray()) { _, which ->
                if (which > 0) {
                    val selected = current.removeAt(which)
                    current.add(0, selected)
                    saveQuickReplies(current)
                    renderQuickReplies()
                }
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun addMessage(text: String, type: MessageType) {
        adapter.addMessage(ChatMessage(text, type))
        rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    private fun showExpandedMessage(message: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.expanded_message_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_accept), null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterEmergencyReceiverIfNeeded()
        tts?.stop()
        tts?.shutdown()
    }

    private fun registerEmergencyReceiverIfNeeded() {
        if (receiverRegistered) return

        val filter = IntentFilter(emergencyAction)
        ContextCompat.registerReceiver(
            this,
            emergencyReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun unregisterEmergencyReceiverIfNeeded() {
        if (!receiverRegistered) return
        unregisterReceiver(emergencyReceiver)
        receiverRegistered = false
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speakId")
    }

    private fun startMicPulse() {
        micPulse = ObjectAnimator.ofFloat(btnMicTranscription, "scaleX", 1f, 1.15f).apply {
            duration = 400
            interpolator = LinearInterpolator()
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        // Also scale Y
        ObjectAnimator.ofFloat(btnMicTranscription, "scaleY", 1f, 1.15f).apply {
            duration = 400
            interpolator = LinearInterpolator()
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        btnMicTranscription.setColorFilter(resources.getColor(R.color.s2r_bubble_usuario, null))
    }

    private fun stopMicPulse() {
        micPulse?.cancel()
        micPulse = null
        btnMicTranscription.scaleX = 1f
        btnMicTranscription.scaleY = 1f
        btnMicTranscription.clearColorFilter()
    }

    private fun startWarningBlink() {
        ObjectAnimator.ofFloat(imgWarning, "alpha", 1f, 0.2f).apply {
            duration = 500
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopWarningBlink() {
        imgWarning.alpha = 1f
    }
}

