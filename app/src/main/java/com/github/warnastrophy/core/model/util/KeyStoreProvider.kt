package com.github.warnastrophy.core.model.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Provides access to the cryptographic key stored in the Android Keystore system. This object
 * manages the creation, retrieval, and fallback behavior for obtaining a cryptographic key used for
 * encryption and decryption operations.
 *
 * If the Android Keystore system is available, it will attempt to load or create a key using the
 * Keystore. If the Keystore is not available (e.g., during unit testing), it falls back to using an
 * in-memory key.
 *
 * This object uses AES encryption with a 256-bit key for key generation and supports GCM block mode
 * and no encryption padding for the key.
 *
 * @see KeyStore
 * @see KeyGenerator
 */
object KeyStoreProvider {

  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "HealthCardKey"

  // fallback used only if keystore is not available (ex: unit tests Robolectric)
  private var inMemoryKey: SecretKey? = null

  /**
   * Retrieves the AES key associated with [KEY_ALIAS], creating it if necessary.
   *
   * @return A [SecretKey] instance suitable for AES-GCM encryption and decryption.
   * @throws Exception if the key cannot be created or retrieved (except during fallback).
   */
  fun getOrCreateKey(): SecretKey {
    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
      if (entry != null) {
        return entry.secretKey
      }

      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

      val parameterSpec =
          KeyGenParameterSpec.Builder(
                  KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
              .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
              .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
              .setRandomizedEncryptionRequired(true)
              .setKeySize(256)
              .build()

      keyGenerator.init(parameterSpec)
      keyGenerator.generateKey()
    } catch (e: Exception) {
      //  fallback for unit tests (AndroidKeyStore not available)
      if (inMemoryKey == null) {
        Log.w("KeyStoreProvider", "Keystore not available, using in-memory key", e)
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        inMemoryKey = keyGenerator.generateKey()
      }
      inMemoryKey!!
    }
  }
}
