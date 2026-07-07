package com.example.speak2read.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
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
        private const val NOTIFICATION_ID = 1001
        private const val MODEL_FILE = "yamnet.tflite"
        private const val PROBABILITY_THRESHOLD = 0.30f 
    }

    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: android.media.AudioRecord? = null
    private var executor: ScheduledExecutorService? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        try {
            audioClassifier = AudioClassifier.createFromFile(this, MODEL_FILE)
            audioRecord = audioClassifier?.createAudioRecord()
            Log.d("S2R_Sound", "IA cargada y lista")
        } catch (e: Exception) {
            Log.e("S2R_Sound", "Error IA: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startForegroundService()
            startDetection()
            isRunning = true
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Detección de Sonidos", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Escuchando Alertas")
            .setContentText("Speak2Read te avisará si escucha alarmas.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startDetection() {
        if (audioClassifier == null || audioRecord == null) return
        audioRecord?.startRecording()
        executor = Executors.newSingleThreadScheduledExecutor()
        
        executor?.scheduleWithFixedDelay({
            try {
                // Obtenemos el tensor de entrada directamente del clasificador
                val tensor = audioClassifier?.createInputAudioTensor()
                tensor?.load(audioRecord)
                
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
                            label.contains("emergency") -> "EMERGENCIA"
                            else -> null
                        }
                        
                        mappedType?.let {
                            Log.d("S2R_Sound", "¡DETECTADO!: $it con $score")
                            sendDetectionBroadcast(it, score)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("S2R_Sound", "Ciclo fallido: ${e.message}")
            }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    private fun sendDetectionBroadcast(soundType: String, confidence: Float) {
        val intent = Intent(ACTION_SOUND_DETECTED).apply {
            putExtra(EXTRA_SOUND_TYPE, soundType)
            putExtra(EXTRA_CONFIDENCE, (confidence * 100).toInt())
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        isRunning = false
        executor?.shutdown()
        audioRecord?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
