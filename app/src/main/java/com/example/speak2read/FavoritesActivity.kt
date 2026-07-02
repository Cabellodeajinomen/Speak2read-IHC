package com.example.speak2read

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.speak2read.adapter.ChatAdapter
import com.example.speak2read.database.Speak2ReadDatabase
import com.example.speak2read.model.ChatMessage
import com.example.speak2read.model.MessageType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity

class FavoritesActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var adapter: ChatAdapter
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var database: Speak2ReadDatabase
    private lateinit var auth: FirebaseAuth
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        auth = FirebaseAuth.getInstance()
        tts = TextToSpeech(this, this)
        rvFavorites = findViewById(R.id.recyclerFavorites)
        emptyState = findViewById(R.id.emptyStateFavorites)
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_favorites

        database = Room.databaseBuilder(applicationContext, Speak2ReadDatabase::class.java, "speak2read_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        adapter = ChatAdapter(
            onExpandMessage = { message -> showExpandedMessage(message) },
            onSpeakMessage = { message -> speakText(message) },
            onFavoriteMessage = { message -> toggleFavorite(message) }
        )
        rvFavorites.layoutManager = LinearLayoutManager(this)
        rvFavorites.adapter = adapter

        loadFavorites()
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); finish(); true }
                R.id.nav_conversations -> { startActivity(Intent(this, ConversationsActivity::class.java)); finish(); true }
                R.id.nav_favorites -> true
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); false }
                else -> false
            }
        }
    }

    private fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: "guest"
        val favorites = database.messageDao().getFavorites(userId)
        val chatMessages = favorites.map {
            ChatMessage(id = it.id, text = it.text, type = if (it.type == "SEND") MessageType.SEND else MessageType.RECEIVE, timestamp = it.timestamp, isFavorite = it.isFavorite)
        }
        adapter.submitMessages(chatMessages)
        
        if (chatMessages.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rvFavorites.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
        }
    }

    private fun toggleFavorite(message: ChatMessage) {
        val newStatus = !message.isFavorite
        database.messageDao().updateFavorite(message.id, newStatus)
        loadFavorites()
        val msg = if (newStatus) "Agregado a favoritos" else "Quitado de favoritos"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showExpandedMessage(message: String) {
        val intent = Intent(this, FullscreenTranscriptionActivity::class.java)
        intent.putExtra("text", message)
        startActivity(intent)
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speakId")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
    }
}
