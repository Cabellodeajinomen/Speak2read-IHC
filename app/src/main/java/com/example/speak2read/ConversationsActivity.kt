package com.example.speak2read

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.speak2read.adapter.ChatAdapter
import com.example.speak2read.adapter.ContactAdapter
import com.example.speak2read.database.Speak2ReadDatabase
import com.example.speak2read.model.ChatMessage
import com.example.speak2read.model.MessageType
import com.google.firebase.auth.FirebaseAuth

class ConversationsActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var rvContacts: RecyclerView
    private lateinit var rvContactsList: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var tvEmptyState: TextView
    private lateinit var tvCurrentFilter: TextView
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
        
        rvContacts = findViewById(R.id.rvContacts)
        rvContactsList = findViewById(R.id.rvContactsList)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvCurrentFilter = findViewById(R.id.tvCurrentFilter)

        chatAdapter = ChatAdapter(
            onExpandMessage = { /* handle */ },
            onSpeakMessage = { /* handle */ },
            onFavoriteMessage = { /* handle */ }
        )
        rvContacts.layoutManager = LinearLayoutManager(this)
        rvContacts.adapter = chatAdapter

        contactAdapter = ContactAdapter { contact ->
            loadByContact(contact)
        }
        rvContactsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvContactsList.adapter = contactAdapter

        setupBottomNavigation()
        setupFilters()
        
        loadAllMessages()
        loadContacts()
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_conversations -> true
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoritesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun loadContacts() {
        val contacts = database.messageDao().getDistinctContacts(currentUserId)
        contactAdapter.submitContacts(contacts)
    }

    private fun setupFilters() {
        findViewById<Button>(R.id.filterHospital).setOnClickListener { loadByCategory("HOSPITAL") }
        findViewById<Button>(R.id.filterTransporte).setOnClickListener { loadByCategory("TRANSPORTE") }
        findViewById<Button>(R.id.filterCompras).setOnClickListener { loadByCategory("COMPRAS") }
        findViewById<Button>(R.id.filterTodo).setOnClickListener { loadAllMessages() }
    }

    private fun loadAllMessages() {
        tvCurrentFilter.text = getString(R.string.label_messages_title)
        val messages = database.messageDao().getAll(currentUserId)
        displayMessages(messages)
    }

    private fun loadByCategory(category: String) {
        val categoryDisplayName = when(category) {
            "HOSPITAL" -> getString(R.string.category_hospital)
            "TRANSPORTE" -> getString(R.string.category_transport)
            "COMPRAS" -> getString(R.string.category_shopping)
            else -> category
        }
        tvCurrentFilter.text = getString(R.string.filter_by_category, categoryDisplayName)
        val messages = database.messageDao().getByCategory(currentUserId, category)
        displayMessages(messages)
    }

    private fun loadByContact(contact: String) {
        tvCurrentFilter.text = getString(R.string.filter_by_contact, contact)
        val messages = database.messageDao().getByContact(currentUserId, contact)
        displayMessages(messages)
    }

    private fun displayMessages(entities: List<com.example.speak2read.database.ChatMessageEntity>) {
        val chatMessages = entities.map {
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
        chatAdapter.submitMessages(chatMessages)
        tvEmptyState.visibility = if (chatMessages.isEmpty()) View.VISIBLE else View.GONE
    }
}
