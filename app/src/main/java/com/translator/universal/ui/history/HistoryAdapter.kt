package com.translator.universal.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.translator.universal.data.local.TranslationHistoryEntity
import com.translator.universal.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (TranslationHistoryEntity) -> Unit,
    private val onDeleteClick: (TranslationHistoryEntity) -> Unit,
    private val onFavoriteClick: (TranslationHistoryEntity) -> Unit
) : ListAdapter<TranslationHistoryEntity, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        
        fun bind(history: TranslationHistoryEntity) {
            binding.apply {
                sourceText.text = history.sourceText
                translatedText.text = history.translatedText
                languagePair.text = "${getLanguageName(history.sourceLanguage)} → ${getLanguageName(history.targetLanguage)}"
                timestamp.text = dateFormat.format(Date(history.timestamp))
                
                offlineIndicator.visibility = if (history.isOffline) 
                    android.view.View.VISIBLE else android.view.View.GONE
                
                root.setOnClickListener { onItemClick(history) }
                deleteButton.setOnClickListener { onDeleteClick(history) }
                favoriteButton.setOnClickListener { onFavoriteClick(history) }
            }
        }
        
        private fun getLanguageName(code: String): String {
            return when (code) {
                "auto" -> "Otomatik"
                "tr" -> "Türkçe"
                "en" -> "İngilizce"
                "de" -> "Almanca"
                "fr" -> "Fransızca"
                "es" -> "İspanyolca"
                "it" -> "İtalyanca"
                "ru" -> "Rusça"
                "ar" -> "Arapça"
                "zh" -> "Çince"
                "ja" -> "Japonca"
                "ko" -> "Korece"
                "pt" -> "Portekizce"
                "nl" -> "Felemenkçe"
                "pl" -> "Lehçe"
                "hi" -> "Hintçe"
                else -> code.uppercase()
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<TranslationHistoryEntity>() {
        override fun areItemsTheSame(oldItem: TranslationHistoryEntity, newItem: TranslationHistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TranslationHistoryEntity, newItem: TranslationHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}