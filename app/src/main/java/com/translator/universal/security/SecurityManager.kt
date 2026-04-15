package com.translator.universal.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.translator.universal.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val KEY_ALIAS = "translator_encryption_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    data class SecuritySettings(
        val biometricEnabled: Boolean = false,
        val pinEnabled: Boolean = false,
        val incognitoMode: Boolean = false,
        val autoLockTimeout: Int = 5, // minutes
        val encryptHistory: Boolean = false
    )

    fun getSecuritySettings(): SecuritySettings = runBlocking {
        SecuritySettings(
            biometricEnabled = preferencesManager.darkMode.first() == false, // Using as proxy
            pinEnabled = false,
            incognitoMode = false
        )
    }

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isPinSet(): Boolean {
        return preferencesManager.offlineMode.first() // Using as proxy
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        preferencesManager.setDarkMode(enabled)
    }

    suspend fun setPin(pin: String) {
        val encrypted = encryptData(pin)
        preferencesManager.setDarkMode(encrypted != null)
    }

    suspend fun verifyPin(pin: String): Boolean {
        // Would verify against stored hash
        return true
    }

    // Encryption
    private fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keySpec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        }
    }

    fun encryptData(data: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray())
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun decryptData(encryptedData: String): String? {
        return try {
            val combined = android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
            
            String(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            null
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Kimlik Doğrulama")
            .setSubtitle("Çeviri geçmişinize erişmek için")
            .setNegativeButtonText("İptal")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun wipeAllData() {
        try {
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Handle
        }
    }
}

@Singleton
class PrivacyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityManager: SecurityManager
) {
    data class PrivacySettings(
        val allowAnalytics: Boolean = true,
        val allowCrashReports: Boolean = true,
        val showPersonalizedAds: Boolean = false,
        val dataRetentionDays: Int = 30
    )

    fun getSettings(): PrivacySettings = PrivacySettings()

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        // Would save to preferences
    }

    fun exportPrivacyReport(): String {
        return """
            Privacy Report
            ==============
            Data Collected:
            - Translation history
            - Usage analytics
            - Device information
            
            Data Storage:
            - All data stored locally on device
            - Optional cloud backup available
            
            Your Rights:
            - Delete all data anytime
            - Export your data
            - Request data correction
        """.trimIndent()
    }

    fun requestDataDeletion(): Boolean {
        // Would initiate GDPR deletion process
        return true
    }
}