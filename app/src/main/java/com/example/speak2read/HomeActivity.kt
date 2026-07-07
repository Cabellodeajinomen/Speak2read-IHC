package com.example.speak2read

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var rvChat: RecyclerView
    private lateinit var adapter: ChatAdapter
    private lateinit var etMessage: EditText
    private lateinit var etTranscription: EditText
    private lateinit var btnSpeaker: ImageButton
    private lateinit var btnMicTranscription: ImageButton
    private lateinit var btnFullscreenTranscription: ImageButton
    private lateinit var btnSendTranscription: ImageButton
    private lateinit var btnSelectContact: ImageButton
    private lateinit var btnSendMessage: ImageButton
    private lateinit var btnAcknowledge: android.widget.Button
    private lateinit var emergencyOverlay: View
    private lateinit var tvEmergencyTitle: TextView
    private lateinit var tvEmergencyType: TextView
    private lateinit var imgWarning: ImageView
    private lateinit var btnSosHeader: View
    private lateinit var btnExitChat: ImageButton
    private lateinit var ivHeaderLogo: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var database: Speak2ReadDatabase
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = "guest"
    private var currentContact: String? = null
    
    private var listening = false
    private var tts: TextToSpeech? = null
    private var micPulse: ObjectAnimator? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val REQUEST_RECORD_AUDIO = 100

    private val emergencyAction = "com.example.speak2read.ACTION_SOUND_DETECTED"
    private var receiverRegistered = false

    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val soundType = intent?.getStringExtra("extra_sound_type") ?: "Alarma detectada"
            val confidence = intent?.getIntExtra("extra_confidence", 0) ?: 0
            showEmergencyOverlay(soundType, confidence)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(true)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        currentUserId = auth.currentUser?.uid ?: "guest"

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
        btnSelectContact = findViewById(R.id.btnSelectContact)
        emergencyOverlay = findViewById(R.id.emergencyOverlay)
        tvEmergencyTitle = findViewById(R.id.tvEmergencyTitle)
        tvEmergencyType = findViewById(R.id.tvEmergencyType)
        btnAcknowledge = findViewById(R.id.btnAcknowledge)
        imgWarning = findViewById(R.id.imgWarning)
        btnSosHeader = findViewById(R.id.btnSosHeader)
        btnExitChat = findViewById(R.id.btnExitChat)
        ivHeaderLogo = findViewById(R.id.ivHeaderLogo)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        bottomNav = findViewById(R.id.bottom_navigation)

        database = Room.databaseBuilder(applicationContext, Speak2ReadDatabase::class.java, "speak2read_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        configureSpeechRecognizer()
        
        adapter = ChatAdapter(
            onExpandMessage = { message -> showExpandedMessage(message) },
            onSpeakMessage = { message -> speakText(message) },
            onFavoriteMessage = { message -> toggleFavorite(message) }
        )
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        loadMessages()
        
        // Manejar apertura de un chat especifico
        val filterContact = intent.getStringExtra("FILTER_CONTACT")
        if (filterContact != null) {
            currentContact = filterContact
            tvHeaderTitle.text = "Chat: $currentContact"
            btnExitChat.visibility = View.VISIBLE
            ivHeaderLogo.visibility = View.GONE
            loadMessages()
        }

        // Quick replies clicks
        findViewById<View>(R.id.qr_si).setOnClickListener { sendQuickReply(findViewById<TextView>(R.id.tvQr1).text.toString()) }
        findViewById<View>(R.id.qr_no).setOnClickListener { sendQuickReply(findViewById<TextView>(R.id.tvQr2).text.toString()) }
        findViewById<View>(R.id.qr_repite).setOnClickListener { sendQuickReply(findViewById<TextView>(R.id.tvQr3).text.toString()) }
        findViewById<View>(R.id.qr_ayuda).setOnClickListener { sendQuickReply(findViewById<TextView>(R.id.tvQr4).text.toString()) }

        btnSosHeader.setOnClickListener { showSosConfirmation() }
        updateQuickReplies(Speak2ReadPrefs.getCurrentContext(this))

        btnSelectContact.setOnClickListener { showContactDialog() }

        btnExitChat.setOnClickListener {
            currentContact = null
            tvHeaderTitle.text = "Speak2Read"
            btnExitChat.visibility = View.GONE
            ivHeaderLogo.visibility = View.VISIBLE
            loadMessages()
            Toast.makeText(this, "Volviste al Chat General", Toast.LENGTH_SHORT).show()
        }

        // Context Buttons
        findViewById<Button>(R.id.btnHospital).setOnClickListener { setAppContext("HOSPITAL") }
        findViewById<Button>(R.id.btnTransporte).setOnClickListener { setAppContext("TRANSPORTE") }
        findViewById<Button>(R.id.btnCompras).setOnClickListener { setAppContext("COMPRAS") }
        findViewById<Button>(R.id.btnEmergencia).setOnClickListener { setAppContext("EMERGENCIA") }
        findViewById<Button>(R.id.btnPersonalizado).setOnClickListener { setAppContext("PERSONALIZADO") }

        // Voice Transcription
        btnMicTranscription.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
                return@setOnClickListener
            }
            
            // IHC: Pausar servicio de alarma para que no robe el microfono
            stopSoundService()

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
        btnSpeaker.setOnClickListener { speakText(etMessage.text.toString()) }

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
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.cancel()
        }

        setupBottomNavigation()
        applyFontScale()

        if (Speak2ReadPrefs.isAlarmDetectionEnabled(this)) {
            checkAndStartSoundService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Actualizar el intent de la actividad
        val filterContact = intent.getStringExtra("FILTER_CONTACT")
        if (filterContact != null) {
            currentContact = filterContact
            tvHeaderTitle.text = "Chat: $currentContact"
            btnExitChat.visibility = View.VISIBLE
            ivHeaderLogo.visibility = View.GONE
            loadMessages()
        }
    }

    private fun checkAndStartSoundService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, com.example.speak2read.service.SoundDetectionService::class.java)
            startForegroundService(intent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        }
    }

    private fun applyFontScale() {
        val scale = Speak2ReadPrefs.fontScale(this)
        etTranscription.textSize = 22f * scale
        etMessage.textSize = 16f * scale
        findViewById<TextView>(R.id.tvQr1).textSize = 14f * scale
        findViewById<TextView>(R.id.tvQr2).textSize = 14f * scale
        findViewById<TextView>(R.id.tvQr3).textSize = 14f * scale
        findViewById<TextView>(R.id.tvQr4).textSize = 14f * scale
    }

    private fun setupBottomNavigation() {
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_conversations -> { 
                    startActivity(Intent(this, ConversationsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    true 
                }
                R.id.nav_favorites -> { 
                    startActivity(Intent(this, FavoritesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    true 
                }
                R.id.nav_settings -> { 
                    startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                    false 
                }
                else -> false
            }
        }
    }

    private fun startSoundService() {
        val intent = Intent(this, com.example.speak2read.service.SoundDetectionService::class.java)
        startForegroundService(intent)
    }

    private fun stopSoundService() {
        val intent = Intent(this, com.example.speak2read.service.SoundDetectionService::class.java)
        stopService(intent)
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

    private fun toggleFavorite(message: ChatMessage) {
        val newStatus = !message.isFavorite
        message.isFavorite = newStatus
        database.messageDao().updateFavorite(message.id, newStatus)
        adapter.notifyDataSetChanged()
        val msg = if (newStatus) "Agregado a favoritos" else "Quitado de favoritos"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
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
                // Reanudar alarma si estaba habilitada
                if (Speak2ReadPrefs.isAlarmDetectionEnabled(this@HomeActivity)) {
                    startSoundService()
                }
            }
            override fun onError(error: Int) {
                stopMicPulse()
                listening = false
                // Reanudar alarma si estaba habilitada
                if (Speak2ReadPrefs.isAlarmDetectionEnabled(this@HomeActivity)) {
                    startSoundService()
                }
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se escuchó nada claro, ¿puedes repetir?"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No detecté sonido, intenta hablar más fuerte."
                    SpeechRecognizer.ERROR_NETWORK -> "Problema de conexión, revisa tu internet."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El sistema está ocupado, espera un segundo."
                    SpeechRecognizer.ERROR_AUDIO -> "Error de audio, intenta de nuevo."
                    else -> "No pude escucharte bien, inténtalo otra vez."
                }
                Toast.makeText(this@HomeActivity, message, Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) etTranscription.setText(matches[0])
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Speak2ReadPrefs.isAlarmDetectionEnabled(this)) {
                checkAndStartSoundService()
            }
            if (listening) {
                btnMicTranscription.performClick()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerEmergencyReceiverIfNeeded()
        // Asegurar que el tab correcto esté seleccionado al volver
        bottomNav.selectedItemId = R.id.nav_home
    }

    override fun onStop() {
        unregisterEmergencyReceiverIfNeeded()
        super.onStop()
    }

    private fun showContactDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_contact, null)
        val tietName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tietContactName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveContact)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelContact)
        
        tietName.setText(currentContact)
        
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val name = tietName.text.toString().trim()
            if (name.isNotEmpty()) {
                currentContact = name
                tvHeaderTitle.text = "Chat: $currentContact"
                btnExitChat.visibility = View.VISIBLE
                ivHeaderLogo.visibility = View.GONE
                Toast.makeText(this, "Ahora hablando con: $currentContact", Toast.LENGTH_SHORT).show()
                loadMessages() 
            } else {
                currentContact = null
                tvHeaderTitle.text = "Speak2Read"
                btnExitChat.visibility = View.GONE
                ivHeaderLogo.visibility = View.VISIBLE
                Toast.makeText(this, "Modo General (Sin contacto)", Toast.LENGTH_SHORT).show()
                loadMessages()
            }
            alertDialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        
        alertDialog.show()
    }

    private fun addMessage(text: String, type: MessageType) {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val category = Speak2ReadPrefs.getCurrentContext(this)
        
        // LOGICA IHC: Si estamos en modo filtro de contacto, guardamos con ese nombre
        val entity = ChatMessageEntity(
            userId = currentUserId,
            text = text,
            type = type.name,
            timestamp = time,
            category = category,
            contactName = currentContact
        )
        val id = database.messageDao().insert(entity).toInt()
        
        // Solo mostramos en pantalla si no hay filtro o si el contacto coincide
        adapter.addMessage(ChatMessage(
            id = id,
            text = text,
            type = type,
            timestamp = time,
            category = category,
            contactName = currentContact
        ))
        rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    private fun loadMessages() {
        val savedMessages = if (currentContact != null) {
            database.messageDao().getMessagesByContact(currentUserId, currentContact!!)
        } else {
            // Modo general: mensajes que NO tienen contacto asignado
            database.messageDao().getAll(currentUserId).filter { it.contactName == null }
        }

        val chatMessages = savedMessages.map {
            ChatMessage(
                id = it.id,
                text = it.text,
                type = if (it.type == "SEND") MessageType.SEND else MessageType.RECEIVE,
                timestamp = it.timestamp,
                isFavorite = it.isFavorite,
                category = it.category,
                contactName = it.contactName
            )
        }
        adapter.submitMessages(chatMessages)
        if (adapter.itemCount > 0) rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    private fun showExpandedMessage(message: String) {
        val intent = Intent(this, FullscreenTranscriptionActivity::class.java)
        intent.putExtra("text", message)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterEmergencyReceiverIfNeeded()
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }

    private fun showEmergencyOverlay(soundType: String, confidence: Int) {
        val displayType = when(soundType) {
            "SIRENA" -> "🚨 SIRENA DETECTADA"
            "INCENDIO" -> "🔥 ¡FUEGO / ALARMA!"
            "BOCINA" -> "📢 BOCINA CERCA"
            "HUMO" -> "💨 DETECTOR DE HUMO"
            "AMBULANCIA" -> "🚑 AMBULANCIA"
            else -> "⚠️ $soundType"
        }
        
        tvEmergencyType.text = "$displayType\n($confidence%)"
        emergencyOverlay.visibility = View.VISIBLE
        
        // VIBRACIÓN AGRESIVA EN BUCLE (500ms vibrar, 100ms pausa, 500ms vibrar...)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        val pattern = longArrayOf(0, 600, 150, 600, 150, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0)) // El 0 significa repetir bucle
        } else {
            vibrator.vibrate(pattern, 0)
        }
        
        startWarningBlink()
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
