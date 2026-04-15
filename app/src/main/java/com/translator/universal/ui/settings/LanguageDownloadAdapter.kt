package com.translator.universal.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.translator.universal.data.model.Language
import com.translator.universal.databinding.ItemLanguageBinding

class LanguageDownloadAdapter(
    private val onActionClick: (String, String) -> Unit
) : ListAdapter<Language, LanguageDownloadAdapter.LanguageViewHolder>(LanguageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: Language) {
            binding.languageName.text = language.name
            binding.languageNative.text = language.nativeName
            
            if (language.isDownloaded) {
                binding.btnAction.text = "Sil"
                binding.btnAction.setOnClickListener { onActionClick(language.code, "delete") }
            } else {
                binding.btnAction.text = "İndir"
                binding.btnAction.setOnClickListener { onActionClick(language.code, "download") }
            }
        }
    }

    class LanguageDiffCallback : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: Language, newItem: Language): Boolean {
            return oldItem == newItem
        }
    }
}