package com.translator.universal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.translator.universal.data.service.NetworkService

class NetworkChangeReceiver : BroadcastReceiver() {
    
    interface NetworkStateListener {
        fun onNetworkChanged(isConnected: Boolean)
    }
    
    private var listener: NetworkStateListener? = null
    
    fun setListener(listener: NetworkStateListener) {
        this.listener = listener
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        val isConnected = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        listener?.onNetworkChanged(isConnected)
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Schedule any background tasks after boot
        }
    }
}

class LanguageModelUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Check for model updates in background
    }
}

class QuickSettingsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle quick settings tile clicks
    }
}