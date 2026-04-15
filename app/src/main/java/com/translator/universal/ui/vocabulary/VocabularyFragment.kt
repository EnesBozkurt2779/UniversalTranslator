package com.translator.universal.ui.vocabulary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.translator.universal.databinding.FragmentVocabularyBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VocabularyFragment : Fragment() {

    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VocabularyViewModel by viewModels()
    private lateinit var vocabularyAdapter: VocabularyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVocabularyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabs()
        observeData()
    }

    private fun setupRecyclerView() {
        vocabularyAdapter = VocabularyAdapter(
            onItemClick = { word ->
                // Show details dialog
            },
            onDeleteClick = { word ->
                viewModel.deleteWord(word)
            },
            onLearnedClick = { word ->
                viewModel.toggleLearned(word)
            }
        )
        
        binding.wordsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = vocabularyAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadAllWords()
                    1 -> viewModel.loadUnlearnedWords()
                    2 -> viewModel.loadLearnedWords()
                    3 -> viewModel.startFlashcardMode()
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordList.collectLatest { list ->
                vocabularyAdapter.submitList(list)
                binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.wordCount.collectLatest { total ->
                viewModel.learnedCount.collectLatest { learned ->
                    binding.progressText.text = "$learned / $total kelime öğrenildi"
                    binding.progressBar.max = total
                    binding.progressBar.progress = learned
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}