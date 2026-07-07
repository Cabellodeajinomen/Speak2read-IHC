package com.example.speak2read.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.speak2read.R

data class ChatItem(
    val name: String,
    val lastMessage: String,
    var isPinned: Boolean = false
)

class ContactAdapter(
    private val onChatClick: (String) -> Unit,
    private val onPinClick: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private var items: List<ChatItem> = emptyList()

    fun submitList(newItems: List<ChatItem>) {
        items = newItems.sortedWith(compareByDescending<ChatItem> { it.isPinned }.thenBy { it.name })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvLastMsg.text = item.lastMessage
        holder.btnPin.setImageResource(if (item.isPinned) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        
        holder.itemView.setOnClickListener { onChatClick(item.name) }
        holder.btnPin.setOnClickListener { onPinClick(item.name, !item.isPinned) }
    }

    override fun getItemCount() = items.size

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvChatName)
        val tvLastMsg: TextView = view.findViewById(R.id.tvChatLastMsg)
        val btnPin: ImageButton = view.findViewById(R.id.btnPinChat)
    }
}
