package com.translator.universal.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.translator.universal.R
import com.translator.universal.data.service.TranslationService
import com.translator.universal.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var languageAdapter: LanguageDownloadAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageList()
        setupSwitches()
        observeState()
    }

    private fun setupLanguageList() {
        languageAdapter = LanguageDownloadAdapter { languageCode, action ->
            when (action) {
                "download" -> viewModel.downloadLanguage(languageCode)
                "delete" -> viewModel.deleteLanguage(languageCode)
            }
        }
        binding.languagesRecyclerView.adapter = languageAdapter
        
        val languages = TranslationService.SUPPORTED_LANGUAGES.filter { it.code != "auto" }
        languageAdapter.submitList(languages)
    }

    private fun setupSwitches() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchAutoDetect.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoDetect(isChecked)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.darkMode.collectLatest { isDark ->
                binding.switchDarkMode.isChecked = isDark
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.autoDetect.collectLatest { isEnabled ->
                binding.switchAutoDetect.isChecked = isEnabled
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.downloadState.collectLatest { state ->
                when (state) {
                    is SettingsViewModel.DownloadState.Downloading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is SettingsViewModel.DownloadState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), R.string.success_downloaded, Toast.LENGTH_SHORT).show()
                        languageAdapter.notifyDataSetChanged()
                    }
                    is SettingsViewModel.DownloadState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.networkStatus.collectLatest { status ->
                binding.networkStatusText.text = if (status) getString(R.string.status_online) else getString(R.string.status_offline)
                binding.networkStatusText.setTextColor(
                    requireContext().getColor(if (status) R.color.online_green else R.color.offline_grey)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}