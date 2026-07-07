package com.example.speak2read.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.speak2read.R
import com.example.speak2read.data.local.Speak2ReadPrefs
import com.example.speak2read.data.model.ChatMessage
import com.example.speak2read.data.model.MessageType

class ChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf(),
    private val onExpandMessage: (String) -> Unit,
    private val onSpeakMessage: (String) -> Unit,
    private val onFavoriteMessage: (ChatMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_RECEIVE = 0
        private const val VIEW_TYPE_SEND = 1
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            MessageType.RECEIVE -> VIEW_TYPE_RECEIVE
            MessageType.SEND -> VIEW_TYPE_SEND
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_RECEIVE) {
            val view = inflater.inflate(R.layout.item_chat_receive, parent, false)
            ReceiveViewHolder(view, onExpandMessage, onSpeakMessage, onFavoriteMessage)
        } else {
            val view = inflater.inflate(R.layout.item_chat_send, parent, false)
            SendViewHolder(view, onExpandMessage, onSpeakMessage, onFavoriteMessage)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is ReceiveViewHolder -> holder.bind(message)
            is SendViewHolder -> holder.bind(message)
        }
    }

    fun submitMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.lastIndex)
    }

    class ReceiveViewHolder(
        itemView: View,
        private val onExpandMessage: (String) -> Unit,
        private val onSpeakMessage: (String) -> Unit,
        private val onFavoriteMessage: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageReceive)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeReceive)
        private val btnExpand: ImageButton = itemView.findViewById(R.id.btnExpandReceive)
        private val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeakReceive)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavoriteReceive)

        fun bind(message: ChatMessage) {
            val scale = Speak2ReadPrefs.fontScale(itemView.context)
            tvMessage.text = message.text
            tvMessage.textSize = 16f * scale
            tvTime.text = message.timestamp
            btnExpand.setOnClickListener { onExpandMessage(message.text) }
            btnSpeak.setOnClickListener { onSpeakMessage(message.text) }
            
            val starIcon = if (message.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            btnFavorite.setImageResource(starIcon)
            btnFavorite.setOnClickListener { onFavoriteMessage(message) }
        }
    }

    class SendViewHolder(
        itemView: View,
        private val onExpandMessage: (String) -> Unit,
        private val onSpeakMessage: (String) -> Unit,
        private val onFavoriteMessage: (ChatMessage) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageSend)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTimeSend)
        private val btnExpand: ImageButton = itemView.findViewById(R.id.btnExpandSend)
        private val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeakSend)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavoriteSend)

        fun bind(message: ChatMessage) {
            val scale = Speak2ReadPrefs.fontScale(itemView.context)
            tvMessage.text = message.text
            tvMessage.textSize = 16f * scale
            tvTime.text = message.timestamp
            btnExpand.setOnClickListener { onExpandMessage(message.text) }
            btnSpeak.setOnClickListener { onSpeakMessage(message.text) }

            val starIcon = if (message.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            btnFavorite.setImageResource(starIcon)
            btnFavorite.setOnClickListener { onFavoriteMessage(message) }
        }
    }
}
