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
        
        if (item.isPinned) {
            holder.btnPin.setImageResource(android.R.drawable.ic_menu_directions)
            holder.btnPin.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.s2r_bubble_usuario))
            holder.btnPin.rotation = -45f // Rotación para efecto de pin clavado
        } else {
            holder.btnPin.setImageResource(android.R.drawable.ic_menu_directions)
            holder.btnPin.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.s2r_text_secondary))
            holder.btnPin.rotation = 0f
        }
        
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
