package com.translator.universal.service

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilities @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class DeviceFeatures(
        val hasCamera: Boolean = false,
        val hasFlash: Boolean = false,
        val hasFrontCamera: Boolean = false,
        val hasMicrophone: Boolean = false,
        val hasSpeaker: Boolean = false,
        val hasBiometrics: Boolean = false,
        val hasNfc: Boolean = false,
        val hasGps: Boolean = false,
        val isFoldable: Boolean = false,
        val supports5G: Boolean = false,
        val cameraCount: Int = 0,
        val maxZoom: Float = 0f
    )

    fun getDeviceFeatures(): DeviceFeatures {
        val packageManager = context.packageManager
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        var hasFlash = false
        var cameraCount = 0
        var maxZoom = 0f

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                
                if (capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) == true) {
                    cameraCount++
                    
                    val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    if (flashAvailable == true) hasFlash = true
                    
                    val zoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
                    if (zoom != null && zoom > maxZoom) maxZoom = zoom
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return DeviceFeatures(
            hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            hasFlash = hasFlash,
            hasFrontCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT),
            hasMicrophone = packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            hasSpeaker = packageManager.hasSystemFeature(PackageManager.FEATURE_SPEAKER),
            hasBiometrics = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT),
            hasNfc = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
            hasGps = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            cameraCount = cameraCount,
            maxZoom = maxZoom
        )
    }

    fun supportsCamera(): Boolean = getDeviceFeatures().hasCamera
    fun supportsFlash(): Boolean = getDeviceFeatures().hasFlash
    fun supportsVoiceInput(): Boolean = getDeviceFeatures().hasMicrophone
    fun supportsBiometrics(): Boolean = getDeviceFeatures().hasBiometrics
}

@Singleton
class PerformanceOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class PowerMode {
        BATTERY_SAVER,
        BALANCED,
        PERFORMANCE
    }

    fun getCurrentMode(): PowerMode {
        return PowerMode.BALANCED
    }

    fun shouldPreloadModels(): Boolean {
        return getCurrentMode() != PowerMode.BATTERY_SAVER
    }

    fun shouldUseHighQuality(): Boolean {
        return getCurrentMode() == PowerMode.PERFORMANCE
    }

    fun getCacheSizeLimit(): Long {
        return when (getCurrentMode()) {
            PowerMode.BATTERY_SAVER -> 50 * 1024 * 1024L
            PowerMode.BALANCED -> 150 * 1024 * 1024L
            PowerMode.PERFORMANCE -> 500 * 1024 * 1024L
        }
    }

    fun getTranslationTimeout(): Long {
        return when (getCurrentMode()) {
            PowerMode.BATTERY_SAVER -> 30000L
            PowerMode.BALANCED -> 15000L
            PowerMode.PERFORMANCE -> 5000L
        }
    }
}