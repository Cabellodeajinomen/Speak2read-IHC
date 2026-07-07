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
import android.os.Vibrator
import android.os.VibratorManager
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
        const val ACTION_STOP_ALARM = "com.example.speak2read.ACTION_STOP_ALARM"
        const val EXTRA_SOUND_TYPE = "extra_sound_type"
        const val EXTRA_CONFIDENCE = "extra_confidence"
        private const val CHANNEL_ID = "sound_detection_channel"
        private const val EMERGENCY_CHANNEL_ID = "emergency_alerts_channel"
        private const val NOTIFICATION_ID = 1001
        private const val EMERGENCY_NOTIFICATION_ID = 911
        private const val MODEL_FILE = "yamnet.tflite"
        private const val PROBABILITY_THRESHOLD = 0.35f 
    }

    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: android.media.AudioRecord? = null
    private var executor: ScheduledExecutorService? = null
    private var isRunning = false
    private var isVibrating = false

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
        if (intent?.action == ACTION_STOP_ALARM) {
            stopEmergency()
            return START_STICKY
        }

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
            
            val emergencyChannel = NotificationChannel(EMERGENCY_CHANNEL_ID, "Alertas Críticas", NotificationManager.IMPORTANCE_HIGH).apply {
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
                // Usamos reflexion para asegurar compatibilidad total
                val createTensorMethod = audioClassifier?.javaClass?.methods?.find { it.name == "createInputAudioTensor" }
                val audioTensor = createTensorMethod?.invoke(audioClassifier)
                
                if (audioTensor != null) {
                    val loadMethod = audioTensor.javaClass.methods.find { it.name == "load" && it.parameterTypes.size == 1 }
                    loadMethod?.invoke(audioTensor, audioRecord)
                    
                    val results = audioClassifier?.classify(audioTensor as TensorAudio) as? List<*>
                    val classifications = results?.firstOrNull()
                    
                    val getCategoriesMethod = classifications?.javaClass?.getMethod("getCategories")
                    val categories = getCategoriesMethod?.invoke(classifications) as? List<*>
                    
                    categories?.forEach { category ->
                        val getLabel = category?.javaClass?.getMethod("getLabel")
                        val getScore = category?.javaClass?.getMethod("getScore")
                        val label = (getLabel?.invoke(category) as? String ?: "").lowercase()
                        val score = getScore?.invoke(category) as? Float ?: 0f
                        
                        if (score >= PROBABILITY_THRESHOLD && !isVibrating) {
                            if (label.contains("siren") || label.contains("alarm") || label.contains("horn")) {
                                handleEmergency(label.uppercase(), score)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("S2R_Sound", "Ciclo IA fallido: ${e.message}")
            }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    private fun handleEmergency(soundType: String, confidence: Float) {
        isVibrating = true
        
        val broadcast = Intent(ACTION_SOUND_DETECTED).apply {
            putExtra(EXTRA_SOUND_TYPE, soundType)
            putExtra(EXTRA_CONFIDENCE, (confidence * 100).toInt())
            setPackage(packageName)
        }
        sendBroadcast(broadcast)

        triggerVibration()
        showEmergencyNotification(soundType)
    }

    private fun triggerVibration() {
        val vibrator = getVibrator()
        val pattern = longArrayOf(0, 1000, 200, 1000, 200, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun showEmergencyNotification(soundType: String) {
        val stopIntent = Intent(this, SoundDetectionService::class.java).apply { action = ACTION_STOP_ALARM }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val fullScreenIntent = Intent(this, HomeActivity::class.java)
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 2, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setContentTitle("🚨 ¡PELIGRO DETECTADO!")
            .setContentText("Sonido: $soundType")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DETENER ALERTA", stopPendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EMERGENCY_NOTIFICATION_ID, notification)
    }

    private fun stopEmergency() {
        isVibrating = false
        getVibrator().cancel()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(EMERGENCY_NOTIFICATION_ID)
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onDestroy() {
        stopEmergency()
        isRunning = false
        executor?.shutdown()
        audioRecord?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
