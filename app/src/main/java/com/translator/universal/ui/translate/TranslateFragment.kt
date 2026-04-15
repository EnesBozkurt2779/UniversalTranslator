package com.translator.universal.ui.translate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.translator.universal.R
import com.translator.universal.data.model.TranslationState
import com.translator.universal.data.service.TranslationService
import com.translator.universal.databinding.FragmentTranslateBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TranslateFragment : Fragment() {

    private var _binding: FragmentTranslateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TranslateViewModel by viewModels()

    private lateinit var sourceLanguageAdapter: ArrayAdapter<String>
    private lateinit var targetLanguageAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageSpinners()
        setupButtons()
        observeState()
    }

    private fun setupLanguageSpinners() {
        val languageNames = TranslationService.SUPPORTED_LANGUAGES.map { it.name }

        sourceLanguageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languageNames)
        binding.sourceLanguageSpinner.adapter = sourceLanguageAdapter
        binding.sourceLanguageSpinner.setSelection(0)

        targetLanguageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languageNames)
        binding.targetLanguageSpinner.adapter = targetLanguageAdapter
        binding.targetLanguageSpinner.setSelection(1)
    }

    private fun setupButtons() {
        binding.btnTranslate.setOnClickListener {
            val sourceText = binding.inputText.text.toString().trim()
            if (sourceText.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen metin girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sourceIndex = binding.sourceLanguageSpinner.selectedItemPosition
            val targetIndex = binding.targetLanguageSpinner.selectedItemPosition

            val sourceLang = TranslationService.SUPPORTED_LANGUAGES[sourceIndex].code
            val targetLang = TranslationService.SUPPORTED_LANGUAGES[targetIndex].code

            viewModel.translate(sourceText, sourceLang, targetLang)
        }

        binding.btnSwap.setOnClickListener {
            val sourcePos = binding.sourceLanguageSpinner.selectedItemPosition
            val targetPos = binding.targetLanguageSpinner.selectedItemPosition
            binding.sourceLanguageSpinner.setSelection(targetPos)
            binding.targetLanguageSpinner.setSelection(sourcePos)

            val inputText = binding.inputText.text.toString()
            val outputText = binding.outputText.text.toString()
            binding.inputText.setText(outputText)
            binding.outputText.text = inputText
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.outputText.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Translation", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), R.string.success_copied, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClear.setOnClickListener {
            binding.inputText.text?.clear()
            binding.outputText.text = ""
        }

        binding.btnSpeak.setOnClickListener {
            val text = binding.outputText.text.toString()
            if (text.isNotEmpty()) {
                val targetIndex = binding.targetLanguageSpinner.selectedItemPosition
                val targetLang = TranslationService.SUPPORTED_LANGUAGES[targetIndex].code
                viewModel.speak(text, targetLang)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.translationState.collectLatest { state ->
                when (state) {
                    is TranslationState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnTranslate.isEnabled = true
                    }
                    is TranslationState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnTranslate.isEnabled = false
                    }
                    is TranslationState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnTranslate.isEnabled = true
                        binding.outputText.text = state.result.translatedText
                        binding.statusText.text = if (state.result.isOffline) getString(R.string.status_offline) else getString(R.string.status_online)
                    }
                    is TranslationState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnTranslate.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    is TranslationState.Downloading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}