package com.translator.universal.ui.dialog

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.translator.universal.R
import com.translator.universal.data.model.ConversationMessage
import com.translator.universal.data.model.TranslationState
import com.translator.universal.data.service.TranslationService
import com.translator.universal.databinding.FragmentDialogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DialogFragment : Fragment() {

    private var _binding: FragmentDialogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DialogViewModel by viewModels()

    private lateinit var messagesAdapter: MessagesAdapter

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(requireContext(), "Mikrofon izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupLanguageSpinners()
        setupButtons()
        observeState()
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter()
        binding.messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = messagesAdapter
        }
    }

    private fun setupLanguageSpinners() {
        val languageNames = TranslationService.SUPPORTED_LANGUAGES
            .filter { it.code != "auto" }
            .map { it.name }

        val sourceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languageNames)
        binding.sourceLanguageSpinner.adapter = sourceAdapter

        val targetAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languageNames)
        binding.targetLanguageSpinner.adapter = targetAdapter
    }

    private fun setupButtons() {
        binding.btnMic.setOnClickListener {
            if (viewModel.isListening.value) {
                stopListening()
            } else {
                checkMicrophonePermission()
            }
        }

        binding.btnSend.setOnClickListener {
            val message = binding.inputMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                val sourceLang = TranslationService.SUPPORTED_LANGUAGES.filter { it.code != "auto" }[binding.sourceLanguageSpinner.selectedItemPosition].code
                val targetLang = TranslationService.SUPPORTED_LANGUAGES.filter { it.code != "auto" }[binding.targetLanguageSpinner.selectedItemPosition].code
                
                viewModel.addMessage(message, sourceLang, targetLang)
                binding.inputMessage.text?.clear()
            }
        }
    }

    private fun checkMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                startListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        val sourceLang = TranslationService.SUPPORTED_LANGUAGES.filter { it.code != "auto" }[binding.sourceLanguageSpinner.selectedItemPosition].code
        viewModel.startListening(sourceLang)
    }

    private fun stopListening() {
        viewModel.stopListening()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.speechText.collectLatest { text ->
                if (text.isNotEmpty() && !text.startsWith("Error:")) {
                    binding.inputMessage.setText(text)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isListening.collectLatest { isListening ->
                binding.btnMic.setImageResource(
                    if (isListening) R.drawable.ic_stop else R.drawable.ic_mic
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.messages.collectLatest { messages ->
                messagesAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.messagesRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.translationState.collectLatest { state ->
                when (state) {
                    is TranslationState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is TranslationState.Success -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is TranslationState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
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