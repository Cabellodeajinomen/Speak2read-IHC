package com.example.speak2read.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.speak2read.HomeActivity
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.support.audio.TensorAudio
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class SoundDetectionService : Service() {

    companion object {
        const val ACTION_SOUND_DETECTED = "com.example.speak2read.ACTION_SOUND_DETECTED"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_CONFIDENCE = "extra_confidence"
        private const val CHANNEL_ID = "sound_detection_channel"
        private const val EMERGENCY_CHANNEL_ID = "emergency_alerts_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EMERGENCY_NOTIFICATION_ID = 911
        private const val MODEL_FILE = "yamnet.tflite"
        private const val PROBABILITY_THRESHOLD = 0.30f 
    }

    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: android.media.AudioRecord? = null
    private var executor: ScheduledExecutorService? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        try {
            audioClassifier = AudioClassifier.createFromFile(this, MODEL_FILE)
            audioRecord = audioClassifier?.createAudioRecord()
        } catch (e: Exception) {
            Log.e("S2R_Sound", "Error IA: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createPersistentNotification())
            startDetection()
            isRunning = true
        }
        return START_STICKY
    }

    private fun createPersistentNotification(): Notification {
        val intent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Oído Activo")
            .setContentText("Speak2Read te cuida en segundo plano.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val channel = NotificationChannel(CHANNEL_ID, "Detección de Sonidos", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
            
            val emergencyChannel = NotificationChannel(EMERGENCY_CHANNEL_ID, "Alertas de Emergencia", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alertas críticas de sirenas y alarmas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 200, 800)
            }
            manager.createNotificationChannel(emergencyChannel)
        }
    }

    private fun startDetection() {
        if (audioClassifier == null || audioRecord == null) return
        audioRecord?.startRecording()
        executor = Executors.newSingleThreadScheduledExecutor()
        
        executor?.scheduleWithFixedDelay({
            try {
                // Forma compatible con la versión 0.4.4
                val tensor = TensorAudio.create(audioClassifier!!.requiredTensorAudioFormat, audioClassifier!!.requiredInputBufferSize.toInt())
                tensor.load(audioRecord)
                val results = audioClassifier?.classify(tensor)
                
                results?.firstOrNull()?.categories?.forEach { category ->
                    val label = category.label.lowercase()
                    val score = category.score
                    
                    if (score >= PROBABILITY_THRESHOLD) {
                        val mappedType = when {
                            label.contains("siren") || label.contains("ambulance") -> "SIRENA"
                            label.contains("fire alarm") -> "INCENDIO"
                            label.contains("smoke detector") -> "HUMO"
                            label.contains("horn") -> "BOCINA"
                            else -> null
                        }
                        
                        mappedType?.let {
                            handleEmergency(it, score)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("S2R_Sound", "Ciclo fallido: ${e.message}")
            }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    private fun handleEmergency(soundType: String, confidence: Float) {
        Log.d("S2R_Sound", "¡ALERTA!: $soundType ($confidence)")
        
        // 1. Notificar a la actividad si esta abierta
        val intent = Intent(ACTION_SOUND_DETECTED).apply {
            putExtra(EXTRA_SOUND_TYPE, soundType)
            putExtra(EXTRA_CONFIDENCE, (confidence * 100).toInt())
            setPackage(packageName)
        }
        sendBroadcast(intent)

        // 2. Vibrar desde el servicio (Funciona en segundo plano)
        triggerVibration()

        // 3. Mostrar notificacion de impacto
        showEmergencyNotification(soundType)
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        
        val pattern = longArrayOf(0, 1000, 200, 1000, 200, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun showEmergencyNotification(soundType: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 ¡PELIGRO DETECTADO!")
            .setContentText("Se escucha una $soundType cerca de ti.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) 
            .build()

        try {
            manager.notify(EMERGENCY_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("S2R_Sound", "Error al mostrar notificacion: ${e.message}")
        }
    }

    override fun onDestroy() {
        isRunning = false
        executor?.shutdown()
        audioRecord?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
