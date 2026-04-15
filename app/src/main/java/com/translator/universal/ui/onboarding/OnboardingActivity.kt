package com.translator.universal.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.translator.universal.R
import com.translator.universal.databinding.ActivityOnboardingBinding
import com.translator.universal.ui.main.MainActivity
import com.translator.universal.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    
    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if onboarding is completed
        // For now, always show onboarding
        
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // No text for dots
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })
    }

    private fun updateButtons(position: Int) {
        when (position) {
            3 -> { // Last page
                binding.nextButton.text = "Başla"
                binding.skipButton.visibility = android.view.View.INVISIBLE
            }
            else -> {
                binding.nextButton.text = "İleri"
                binding.skipButton.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun setupButtons() {
        binding.skipButton.setOnClickListener {
            finishOnboarding()
        }

        binding.nextButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < 3) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun finishOnboarding() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

class OnboardingPagerAdapter(activity: AppCompatActivity) : 
    androidx.viewpager2.widget.ViewPager2.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {
    
    private val titles = arrayOf(
        "Evrensel Çevirmen",
        "30+ Dil Desteği",
        "İnternet Yoksa Bile",
        "Başlayalım"
    )
    
    private val descriptions = arrayOf(
        "Tüm dillerde ücretsiz ve sınırsız çeviri deneyimi. En yüksek doğruluk oranıyla metinlerinizi, fotoğraflarınızı ve konuşmalarınızı çevirin.",
        "Türkçe, İngilizce, Almanca, Fransızca, Japonca, Çince ve daha birçok dil... İstediğiniz dilde çeviri yapın.",
        "İnternet bağlantısı olmadan da çeviri yapın. İndirdiğiniz dil modelleriyle offline olarak kullanın.",
        "Artık tüm özelliklerin keyfini çıkarabilirsiniz. Hızlı, güvenilir ve ücretsiz!"
    )
    
    private val images = arrayOf(
        R.drawable.onboarding_1,
        R.drawable.onboarding_2,
        R.drawable.onboarding_3,
        R.drawable.onboarding_4
    )

    override fun getItemCount(): Int = 4

    override fun onCreateViewHolder(container: android.view.ViewGroup, position: Int): OnboardingViewHolder {
        val binding = com.translator.universal.databinding.ItemOnboardingPageBinding.inflate(
            android.view.LayoutInflater.from(container.context), container, false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(titles[position], descriptions[position], images[position])
    }

    inner class OnboardingViewHolder(
        private val binding: com.translator.universal.databinding.ItemOnboardingPageBinding
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        fun bind(title: String, description: String, imageRes: Int) {
            binding.titleText.text = title
            binding.descriptionText.text = description
            binding.imageView.setImageResource(imageRes)
        }
    }
}