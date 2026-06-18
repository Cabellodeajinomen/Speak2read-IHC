package com.example.speak2read.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Servicio base de detección de sonidos para pantallas XML.
 */

class SoundDetectionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    // Invoca este metodo cuando el detector reconozca una alarma o sirena.
    fun onEmergencySoundDetected() {
        // Enviar broadcast para Activities XML
        val intent = Intent("com.example.speak2read.ACTION_EMERGENCY")
        sendBroadcast(intent)
    }
}