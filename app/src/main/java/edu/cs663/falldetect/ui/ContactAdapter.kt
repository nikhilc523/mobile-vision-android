package edu.cs663.falldetect.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs663.falldetect.R
import edu.cs663.falldetect.util.EmergencyContact

/**
 * RecyclerView adapter for displaying emergency contacts.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class ContactAdapter(
    private val onDeleteClick: (EmergencyContact, Int) -> Unit
) : ListAdapter<EmergencyContact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_contact_item, parent, false)
        return ContactViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    /**
     * ViewHolder for contact items.
     */
    class ContactViewHolder(
        itemView: View,
        private val onDeleteClick: (EmergencyContact, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameText: TextView = itemView.findViewById(R.id.text_contact_name)
        private val phoneText: TextView = itemView.findViewById(R.id.text_contact_phone)
        private val deleteButton: ImageView = itemView.findViewById(R.id.btn_delete_contact)

        fun bind(contact: EmergencyContact, position: Int) {
            nameText.text = contact.name
            phoneText.text = contact.phone

            // Set content description for accessibility
            itemView.contentDescription = itemView.context.getString(
                R.string.contact_item_desc,
                contact.name,
                contact.phone
            )

            // Delete button click listener
            deleteButton.setOnClickListener {
                onDeleteClick(contact, position)
            }

            // Set content description for delete button
            deleteButton.contentDescription = itemView.context.getString(
                R.string.delete_contact_desc,
                contact.name
            )
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    private class ContactDiffCallback : DiffUtil.ItemCallback<EmergencyContact>() {
        override fun areItemsTheSame(
            oldItem: EmergencyContact,
            newItem: EmergencyContact
        ): Boolean {
            // Use phone as unique identifier
            return oldItem.phone == newItem.phone
        }

        override fun areContentsTheSame(
            oldItem: EmergencyContact,
            newItem: EmergencyContact
        ): Boolean {
            return oldItem == newItem
        }
    }
}

