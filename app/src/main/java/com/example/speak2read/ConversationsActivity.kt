package com.example.speak2read

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.speak2read.adapter.ChatItem
import com.example.speak2read.adapter.ContactAdapter
import com.example.speak2read.database.Speak2ReadDatabase
import com.google.firebase.auth.FirebaseAuth

class ConversationsActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvChats: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var tvEmptyState: TextView
    private lateinit var database: Speak2ReadDatabase
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String = "guest"

    override fun onCreate(savedInstanceState: Bundle?) {
        Speak2ReadPrefs.applySettings(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations)

        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: "guest"
        
        database = Room.databaseBuilder(applicationContext, Speak2ReadDatabase::class.java, "speak2read_db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_conversations
        
        rvChats = findViewById(R.id.rvContactsList)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        contactAdapter = ContactAdapter(
            onChatClick = { contactName -> openSpecificChat(contactName) },
            onPinClick = { contactName, shouldPin -> togglePin(contactName, shouldPin) }
        )
        
        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = contactAdapter

        loadChats()
        setupBottomNavigation()
    }

    private fun loadChats() {
        val contacts = database.messageDao().getUniqueContacts(currentUserId)
        val chatItems = contacts.map { name ->
            val messages = database.messageDao().getMessagesByContact(currentUserId, name)
            ChatItem(
                name = name,
                lastMessage = messages.lastOrNull()?.text ?: "",
                isPinned = messages.firstOrNull()?.isPinned ?: false
            )
        }

        contactAdapter.submitList(chatItems)
        tvEmptyState.visibility = if (chatItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openSpecificChat(contactName: String) {
        // Enviar a Home pero filtrando por este contacto
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("FILTER_CONTACT", contactName)
        startActivity(intent)
    }

    private fun togglePin(contactName: String, shouldPin: Boolean) {
        database.messageDao().updatePinnedStatus(currentUserId, contactName, shouldPin)
        loadChats()
        val msg = if (shouldPin) "Chat fijado" else "Chat desfijado"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); finish(); true }
                R.id.nav_conversations -> true
                R.id.nav_favorites -> { startActivity(Intent(this, FavoritesActivity::class.java)); finish(); true }
                R.id.nav_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }
}
