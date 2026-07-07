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
        private const val PROBABILITY_THRESHOLD = 0.45f
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
        } catch (e: Exception) {
            Log.e("SoundDetectionService", "Error TFLite: ${e.message}")
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
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Escuchando Alertas")
            .setContentText("Speak2Read utiliza IA para protegerte.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Detección de Sonidos", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startDetection() {
        if (audioClassifier == null || audioRecord == null) return
        audioRecord?.startRecording()
        executor = Executors.newSingleThreadScheduledExecutor()
        
        executor?.scheduleWithFixedDelay({
            try {
                // Usamos reflexión para máxima compatibilidad entre versiones de la librería Audio Task
                val createTensorMethod = audioClassifier?.javaClass?.methods?.find { it.name == "createInputAudioTensor" }
                val audioTensor = createTensorMethod?.invoke(audioClassifier)
                
                if (audioTensor != null) {
                    val loadMethod = audioTensor.javaClass.methods.find { it.name == "load" && it.parameterTypes.size == 1 }
                    loadMethod?.invoke(audioTensor, audioRecord)
                    
                    val results = audioClassifier?.classify(audioTensor as org.tensorflow.lite.support.audio.TensorAudio) as? List<*>
                    val classifications = results?.firstOrNull()
                    
                    val getCategoriesMethod = classifications?.javaClass?.getMethod("getCategories")
                    val categories = getCategoriesMethod?.invoke(classifications) as? List<*>
                    
                    val topCategory = categories?.find { category ->
                        val getLabel = category?.javaClass?.getMethod("getLabel")
                        val getScore = category?.javaClass?.getMethod("getScore")
                        val label = getLabel?.invoke(category) as? String ?: ""
                        val score = getScore?.invoke(category) as? Float ?: 0f
                        
                        val lowerLabel = label.lowercase()
                        (lowerLabel.contains("siren") || 
                         lowerLabel.contains("alarm") || 
                         lowerLabel.contains("horn") || 
                         lowerLabel.contains("smoke detector") ||
                         lowerLabel.contains("emergency") ||
                         lowerLabel.contains("ambulance")) && score >= PROBABILITY_THRESHOLD
                    }

                    topCategory?.let { category ->
                        val getLabel = category.javaClass.getMethod("getLabel")
                        val getScore = category.javaClass.getMethod("getScore")
                        val label = getLabel.invoke(category) as String
                        val score = getScore.invoke(category) as Float
                        
                        val mappedType = when {
                            label.lowercase().contains("fire") -> "INCENDIO"
                            label.lowercase().contains("smoke") -> "HUMO"
                            label.lowercase().contains("siren") -> "SIRENA"
                            label.lowercase().contains("horn") -> "BOCINA"
                            label.lowercase().contains("ambulance") -> "AMBULANCIA"
                            else -> label.uppercase()
                        }
                        sendDetectionBroadcast(mappedType, score)
                    }
                }
            } catch (e: Exception) {
                Log.e("SoundDetectionService", "IA Error: ${e.message}")
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
        audioRecord?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
