package com.translator.universal.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.translator.universal.data.model.ConversationMessage
import com.translator.universal.databinding.ItemMessageBinding

class MessagesAdapter : ListAdapter<ConversationMessage, MessagesAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ConversationMessage) {
            binding.messageText.text = if (message.isFromUser) message.text else message.text
            binding.messageText.textAlignment = if (message.isFromUser) android.view.TextAlignment.TEXT_START else android.view.TextAlignment.TEXT_END
            
            binding.messageCard.setCardBackgroundColor(
                binding.root.context.getColor(
                    if (message.isFromUser) com.translator.universal.R.color.primary_light else com.translator.universal.R.color.card_background
                )
            )
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ConversationMessage>() {
        override fun areItemsTheSame(oldItem: ConversationMessage, newItem: ConversationMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConversationMessage, newItem: ConversationMessage): Boolean {
            return oldItem == newItem
        }
    }
}