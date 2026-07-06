package com.example.speak2read.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.speak2read.R

class ContactAdapter(
    private val contacts: MutableList<String> = mutableListOf(),
    private val onContactClick: (String) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    fun submitContacts(newContacts: List<String>) {
        contacts.clear()
        contacts.addAll(newContacts)
        notifyDataSetChanged()
    }

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(contact: String) {
            text1.text = itemView.context.getString(R.string.contact_display_name, contact)
            text1.textSize = 16f
            text1.setPadding(40, 24, 40, 24)
            itemView.setOnClickListener { onContactClick(contact) }
        }
    }
}
