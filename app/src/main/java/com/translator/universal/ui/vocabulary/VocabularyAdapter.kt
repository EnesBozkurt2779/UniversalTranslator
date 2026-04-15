package com.translator.universal.ui.vocabulary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.translator.universal.data.local.VocabularyEntity
import com.translator.universal.databinding.ItemVocabularyBinding

class VocabularyAdapter(
    private val onItemClick: (VocabularyEntity) -> Unit,
    private val onDeleteClick: (VocabularyEntity) -> Unit,
    private val onLearnedClick: (VocabularyEntity) -> Unit
) : ListAdapter<VocabularyEntity, VocabularyAdapter.VocabularyViewHolder>(VocabularyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VocabularyViewHolder {
        val binding = ItemVocabularyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VocabularyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VocabularyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VocabularyViewHolder(private val binding: ItemVocabularyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(word: VocabularyEntity) {
            binding.apply {
                wordText.text = word.word
                translationText.text = word.translation
                languagePair.text = "${word.sourceLanguage.uppercase()} → ${word.targetLanguage.uppercase()}"
                
                learnedBadge.visibility = if (word.isLearned) 
                    android.view.View.VISIBLE else android.view.View.GONE
                
                learnedButton.setImageResource(
                    if (word.isLearned) android.R.drawable.checkbox_on_background 
                    else android.R.drawable.checkbox_off_background
                )
                
                root.setOnClickListener { onItemClick(word) }
                deleteButton.setOnClickListener { onDeleteClick(word) }
                learnedButton.setOnClickListener { onLearnedClick(word) }
            }
        }
    }

    class VocabularyDiffCallback : DiffUtil.ItemCallback<VocabularyEntity>() {
        override fun areItemsTheSame(oldItem: VocabularyEntity, newItem: VocabularyEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VocabularyEntity, newItem: VocabularyEntity): Boolean {
            return oldItem == newItem
        }
    }
}