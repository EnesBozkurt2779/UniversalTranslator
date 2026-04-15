package com.translator.universal.ui.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.translator.universal.R
import com.translator.universal.data.model.TranslationState
import com.translator.universal.data.service.TranslationService
import com.translator.universal.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CameraViewModel by viewModels()

    private var imageCapture: ImageCapture? = null
    private var isCameraInitialized = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processImage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageSpinner()
        setupButtons()
        checkCameraPermission()
        observeState()
    }

    private fun setupLanguageSpinner() {
        val languageNames = TranslationService.SUPPORTED_LANGUAGES
            .filter { it.code != "auto" }
            .map { it.name }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, languageNames)
        binding.targetLanguageSpinner.adapter = adapter
        binding.targetLanguageSpinner.setSelection(0)
    }

    private fun setupButtons() {
        binding.btnCapture.setOnClickListener {
            captureImage()
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnTranslate.setOnClickListener {
            val recognizedText = binding.recognizedText.text.toString()
            if (recognizedText.isNotEmpty()) {
                val targetIndex = binding.targetLanguageSpinner.selectedItemPosition
                val targetLang = TranslationService.SUPPORTED_LANGUAGES.filter { it.code != "auto" }[targetIndex].code
                viewModel.translateText(recognizedText, targetLang)
            }
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.translatedText.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Translation", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), R.string.success_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                isCameraInitialized = true
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Kamera başlatılamadı", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        binding.progressBar.visibility = View.VISIBLE

        viewModel.captureAndTranslate(imageCapture, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recognizedText.collectLatest { text ->
                binding.recognizedText.text = text
                if (text.isNotEmpty()) {
                    binding.btnTranslate.isEnabled = true
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
                        binding.translatedText.text = state.result.translatedText
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