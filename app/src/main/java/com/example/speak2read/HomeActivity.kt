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
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.example.speak2read.adapter.ChatAdapter
import com.example.speak2read.model.ChatMessage
import com.example.speak2read.model.MessageType
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.speak2read.database.ChatMessageEntity
import com.example.speak2read.database.Speak2ReadDatabase

class HomeActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var rvChat: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var etMessage: EditText
    private lateinit var etTranscription: EditText
    private lateinit var btnSpeaker: ImageButton
    private lateinit var btnMicTranscription: ImageButton
    private lateinit var btnFullscreenTranscription: ImageButton
    private lateinit var btnSendTranscription: ImageButton
    private lateinit var btnSendMessage: ImageButton
    private lateinit var btnAcknowledge: android.widget.Button
    private lateinit var emergencyOverlay: View
    private lateinit var imgWarning: ImageView
    private lateinit var btnSosHeader: View

    private lateinit var database: Speak2ReadDatabase
    private var listening = false
    private var tts: TextToSpeech? = null
    private var micPulse: ObjectAnimator? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val REQUEST_RECORD_AUDIO = 100

    private val emergencyAction = "com.example.speak2read.ACTION_EMERGENCY"
    private var receiverRegistered = false

    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            emergencyOverlay.visibility = View.VISIBLE
            startWarningBlink()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(true)
        setContentView(R.layout.activity_home)

        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

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
        btnSosHeader = findViewById(R.id.btnSosHeader)

        database = Room.databaseBuilder(
            applicationContext,
            Speak2ReadDatabase::class.java,
            "speak2read_db"
        )
            .allowMainThreadQueries()
            .build()

        configureSpeechRecognizer()
        adapter = ChatAdapter(
            onExpandMessage = { message -> showExpandedMessage(message) },
            onSpeakMessage = { message -> speakText(message) }
        )
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        loadMessages()

        // Quick replies clicks
        findViewById<View>(R.id.qr_si).setOnClickListener {
            sendQuickReply(findViewById<TextView>(R.id.tvQr1).text.toString())
        }
        findViewById<View>(R.id.qr_no).setOnClickListener {
            sendQuickReply(findViewById<TextView>(R.id.tvQr2).text.toString())
        }
        findViewById<View>(R.id.qr_repite).setOnClickListener {
            sendQuickReply(findViewById<TextView>(R.id.tvQr3).text.toString())
        }
        findViewById<View>(R.id.qr_ayuda).setOnClickListener {
            sendQuickReply(findViewById<TextView>(R.id.tvQr4).text.toString())
        }

        // SOS Button Header
        btnSosHeader.setOnClickListener {
            showSosConfirmation()
        }

        // Context Initial load
        updateQuickReplies(Speak2ReadPrefs.getCurrentContext(this))

        // Settings Button
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Context Buttons
        findViewById<Button>(R.id.btnHospital).setOnClickListener {
            setAppContext("HOSPITAL")
        }
        findViewById<Button>(R.id.btnTransporte).setOnClickListener {
            setAppContext("TRANSPORTE")
        }
        findViewById<Button>(R.id.btnCompras).setOnClickListener {
            setAppContext("COMPRAS")
        }
        findViewById<Button>(R.id.btnEmergencia).setOnClickListener {
            setAppContext("EMERGENCIA")
        }
        findViewById<Button>(R.id.btnPersonalizado).setOnClickListener {
            setAppContext("PERSONALIZADO")
        }

        // Voice Transcription
        btnMicTranscription.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
                return@setOnClickListener
            }
            startMicPulse()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            }
            speechRecognizer?.startListening(intent)
            listening = true
        }

        btnMicTranscription.setOnLongClickListener {
            etTranscription.setText(getString(R.string.home_transcription_sample))
            true
        }

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

        // My Messages
        btnSpeaker.setOnClickListener {
            speakText(etMessage.text.toString())
        }

        btnSendMessage.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, MessageType.SEND)
                speakText(text)
                etMessage.text?.clear()
            }
        }

        btnAcknowledge.setOnClickListener {
            emergencyOverlay.visibility = View.GONE
            stopWarningBlink()
        }
    }

    private fun setAppContext(ctx: String) {
        Speak2ReadPrefs.setCurrentContext(this, ctx)
        updateQuickReplies(ctx)
    }

    private fun showSosConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Alerta SOS")
            .setMessage("¿Estás seguro de que quieres enviar un mensaje de ayuda urgente?")
            .setPositiveButton("ENVIAR SOS") { _, _ ->
                sendQuickReply("¡NECESITO AYUDA URGENTE! (Alerta SOS)")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configureSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                stopMicPulse()
                listening = false
            }
            override fun onError(error: Int) {
                stopMicPulse()
                listening = false
                Toast.makeText(this@HomeActivity, "Error STT: $error", Toast.LENGTH_LONG).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    etTranscription.setText(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            btnMicTranscription.performClick()
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

    private fun addMessage(text: String, type: MessageType) {
        adapter.addMessage(ChatMessage(text, type))
        database.messageDao().insert(ChatMessageEntity(text = text, type = type.name))
        rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    private fun loadMessages() {
        val savedMessages = database.messageDao().getAll()
        savedMessages.forEach {
            adapter.addMessage(ChatMessage(it.text, if (it.type == "SEND") MessageType.SEND else MessageType.RECEIVE))
        }
        if (adapter.itemCount > 0) rvChat.scrollToPosition(adapter.itemCount - 1)
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
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    private fun registerEmergencyReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter(emergencyAction)
        ContextCompat.registerReceiver(this, emergencyReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private fun unregisterEmergencyReceiverIfNeeded() {
        if (!receiverRegistered) return
        unregisterReceiver(emergencyReceiver)
        receiverRegistered = false
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
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
        ObjectAnimator.ofFloat(btnMicTranscription, "scaleY", 1f, 1.15f).apply {
            duration = 400
            interpolator = LinearInterpolator()
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        btnMicTranscription.setColorFilter(ContextCompat.getColor(this, R.color.s2r_bubble_usuario))
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

    private fun sendQuickReply(text: String) {
        addMessage(text, MessageType.SEND)
        speakText(text)
    }

    private fun updateQuickReplies(context: String) {
        val tvQr1 = findViewById<TextView>(R.id.tvQr1)
        val tvQr2 = findViewById<TextView>(R.id.tvQr2)
        val tvQr3 = findViewById<TextView>(R.id.tvQr3)
        val tvQr4 = findViewById<TextView>(R.id.tvQr4)

        when(context) {
            "HOSPITAL" -> {
                tvQr1.text = "Tengo una cita"
                tvQr2.text = "Necesito ayuda"
                tvQr3.text = "¿Dónde debo ir?"
                tvQr4.text = "Repita por favor"
            }
            "TRANSPORTE" -> {
                tvQr1.text = "¿Qué bus debo tomar?"
                tvQr2.text = "¿Cuánto cuesta?"
                tvQr3.text = "¿Dónde bajo?"
                tvQr4.text = "Gracias"
            }
            "COMPRAS" -> {
                tvQr1.text = "¿Cuánto cuesta?"
                tvQr2.text = "Quiero comprar esto"
                tvQr3.text = "¿Acepta tarjeta?"
                tvQr4.text = "Gracias"
            }
            "EMERGENCIA" -> {
                tvQr1.text = "Necesito ayuda"
                tvQr2.text = "Llame a emergencias"
                tvQr3.text = "Estoy perdido"
                tvQr4.text = "Repita por favor"
            }
            "PERSONALIZADO" -> {
                val custom = Speak2ReadPrefs.getCustomReplies(this)
                tvQr1.text = custom[0]
                tvQr2.text = custom[1]
                tvQr3.text = custom[2]
                tvQr4.text = custom[3]
            }
            else -> {
                tvQr1.text = "Sí"
                tvQr2.text = "No"
                tvQr3.text = "Repita"
                tvQr4.text = "Ayuda"
            }
        }
    }
}
