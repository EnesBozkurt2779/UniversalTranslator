package com.translator.universal.ui.main

import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.translator.universal.R
import com.translator.universal.databinding.ActivityMainBinding
import com.translator.universal.ui.camera.CameraFragment
import com.translator.universal.ui.dialog.DialogFragment
import com.translator.universal.ui.settings.SettingsFragment
import com.translator.universal.ui.translate.TranslateFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentFragmentId = R.id.navText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupImmersiveMode()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupNavigation()
        loadFragment(TranslateFragment())
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupWindowInsets() {
        binding.root.setOnApplyWindowInsets { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
            binding.navHostFragment.setPadding(insets.left, insets.top, insets.right, 0)
            binding.bottomNavigation.setPadding(insets.left, 8, insets.right, insets.bottom + 8)
            windowInsets
        }
    }

    private fun setupNavigation() {
        val navItems = listOf(
            binding.navText to R.id.translateFragment,
            binding.navCamera to R.id.cameraFragment,
            binding.navDialog to R.id.dialogFragment,
            binding.navSettings to R.id.settingsFragment
        )

        navItems.forEach { (view, fragmentId) ->
            view.setOnClickListener {
                if (currentFragmentId != fragmentId) {
                    currentFragmentId = fragmentId
                    updateNavSelection()
                    loadFragmentById(fragmentId)
                }
            }
        }
        
        updateNavSelection()
    }

    private fun updateNavSelection() {
        val selectedColor = getColor(R.color.primary)
        val unselectedColor = getColor(R.color.text_hint_light)
        
        listOf(
            binding.navText to R.id.translateFragment,
            binding.navCamera to R.id.cameraFragment,
            binding.navDialog to R.id.dialogFragment,
            binding.navSettings to R.id.settingsFragment
        ).forEach { (view, fragmentId) ->
            val imageView = view.getChildAt(0) as? android.widget.ImageView
            val textView = view.getChildAt(1) as? android.widget.TextView
            
            if (fragmentId == currentFragmentId) {
                imageView?.setColorFilter(selectedColor)
                textView?.setTextColor(selectedColor)
                view.setBackgroundResource(R.drawable.nav_item_selected_background)
            } else {
                imageView?.setColorFilter(unselectedColor)
                textView?.setTextColor(unselectedColor)
                view.setBackgroundResource(R.drawable.nav_item_background)
            }
        }
    }

    private fun loadFragmentById(fragmentId: Int) {
        val fragment = when (fragmentId) {
            R.id.translateFragment -> TranslateFragment()
            R.id.cameraFragment -> CameraFragment()
            R.id.dialogFragment -> DialogFragment()
            R.id.settingsFragment -> SettingsFragment()
            else -> TranslateFragment()
        }
        loadFragment(fragment)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.navHostFragment, fragment)
            .commit()
    }

    fun updateNetworkStatus(isOnline: Boolean) {
        binding.networkBanner.visibility = if (isOnline) View.GONE else View.VISIBLE
        
        val statusText = if (isOnline) getString(R.string.status_online) else getString(R.string.status_offline)
        binding.networkStatusText.text = statusText
    }

    fun getCurrentFragmentId(): Int = currentFragmentId
}