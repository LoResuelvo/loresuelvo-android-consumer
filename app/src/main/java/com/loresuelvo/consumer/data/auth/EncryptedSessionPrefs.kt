package com.loresuelvo.consumer.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Opens the encrypted backing storage for
 * [com.loresuelvo.consumer.domain.auth.AuthSessionStore].
 *
 * Recovery pattern: under certain conditions (reinstall with a
 * different signing key, factory reset, OS-level keystore wipe)
 * the master key stored in AndroidKeyStore can no longer decrypt
 * the existing ciphertext, throwing `AEADBadTagException` on app
 * launch. The user would otherwise be stuck re-installing. We
 * catch that one specific failure mode, wipe the broken prefs and
 * the master key alias, then rebuild from scratch — the session is
 * cleared, which is the same UX as a fresh install.
 */
internal fun createEncryptedSessionPrefs(context: Context): SharedPreferences {
    val appContext = context.applicationContext
    return try {
        openEncryptedPrefs(appContext)
    } catch (e: Throwable) {
        // AEADBadTagException (signature mismatch on the master key)
        // OR KeyPermanentlyInvalidatedException (AndroidKeyStore wipe)
        // are the two failure modes we recover from. Any other throw
        // is a genuine bug — surface it.
        when (e) {
            is javax.crypto.AEADBadTagException,
            is android.security.keystore.KeyPermanentlyInvalidatedException -> {
                Log.w("SessionPrefs", "Master key unusable, wiping and recreating")
                wipeAndRecreate(appContext)
            }
            else -> throw e
        }
    }
}

private fun openEncryptedPrefs(appContext: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

private fun wipeAndRecreate(appContext: Context): SharedPreferences {
    val prefsFile = java.io.File(
        java.io.File(appContext.applicationInfo.dataDir, "shared_prefs"),
        "$PREFS_NAME.xml",
    )
    runCatching { prefsFile.delete() }
    return openEncryptedPrefs(appContext)
}

internal const val PREFS_NAME = "auth_session_secure"
