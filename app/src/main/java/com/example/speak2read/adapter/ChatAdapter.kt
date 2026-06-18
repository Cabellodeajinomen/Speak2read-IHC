package com.example.speak2read.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.speak2read.R
import com.example.speak2read.model.ChatMessage
import com.example.speak2read.model.MessageType

class ChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf(),
    private val onExpandMessage: (String) -> Unit,
    private val onSpeakMessage: (String) -> Unit
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
            ReceiveViewHolder(view, onExpandMessage, onSpeakMessage)
        } else {
            val view = inflater.inflate(R.layout.item_chat_send, parent, false)
            SendViewHolder(view, onExpandMessage, onSpeakMessage)
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
        private val onSpeakMessage: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageReceive)
        private val btnExpand: ImageButton = itemView.findViewById(R.id.btnExpandReceive)
        private val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeakReceive)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            btnExpand.setOnClickListener { onExpandMessage(message.text) }
            btnSpeak.setOnClickListener { onSpeakMessage(message.text) }
        }
    }

    class SendViewHolder(
        itemView: View,
        private val onExpandMessage: (String) -> Unit,
        private val onSpeakMessage: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageSend)
        private val btnExpand: ImageButton = itemView.findViewById(R.id.btnExpandSend)
        private val btnSpeak: ImageButton = itemView.findViewById(R.id.btnSpeakSend)

        fun bind(message: ChatMessage) {
            tvMessage.text = message.text
            btnExpand.setOnClickListener { onExpandMessage(message.text) }
            btnSpeak.setOnClickListener { onSpeakMessage(message.text) }
        }
    }
}

