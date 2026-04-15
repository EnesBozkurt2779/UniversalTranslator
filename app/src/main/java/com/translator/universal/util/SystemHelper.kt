package com.translator.universal.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    data class NetworkInfo(
        val isConnected: Boolean,
        val isWifi: Boolean,
        val isMobile: Boolean,
        val isMetered: Boolean,
        val connectionType: ConnectionType
    )

    enum class ConnectionType {
        WIFI, MOBILE, ETHERNET, VPN, NONE
    }

    fun getNetworkInfo(): NetworkInfo {
        val network = connectivityManager.activeNetwork ?: return NetworkInfo(false, false, false, false, ConnectionType.NONE)
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo(false, false, false, false, ConnectionType.NONE)

        return NetworkInfo(
            isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
            isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            isMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            connectionType = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> ConnectionType.VPN
                else -> ConnectionType.NONE
            }
        )
    }

    fun isConnected(): Boolean = getNetworkInfo().isConnected
    fun isWifi(): Boolean = getNetworkInfo().isWifi
    fun isMobileData(): Boolean = getNetworkInfo().isMobile
}

@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(permissions: List<String>): Boolean {
        return permissions.all { hasPermission(it) }
    }

    fun getMissingPermissions(permissions: List<String>): List<String> {
        return permissions.filter { !hasPermission(it) }
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    fun getNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            getAppSettingsIntent()
        }
    }

    fun getLocationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    fun getAllInstalledBrowsers(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
    }
}

@Singleton
class AppShortcutsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val SHORTCUT_TRANSLATE = "shortcut_translate"
        const val SHORTCUT_CAMERA = "shortcut_camera"
        const val SHORTCUT_VOICE = "shortcut_voice"
        const val SHORTCUT_HISTORY = "shortcut_history"
    }

    fun getPinnedShortcuts(): List<String> {
        // Would implement actual pinned shortcuts management
        return listOf(SHORTCUT_TRANSLATE, SHORTCUT_CAMERA, SHORTCUT_VOICE)
    }

    fun requestPinShortcut(shortcutId: String): Boolean {
        // Implementation would use ShortcutManager
        return true
    }

    fun removeShortcut(shortcutId: String): Boolean {
        return true
    }
}