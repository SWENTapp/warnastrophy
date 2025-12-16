package com.github.warnastrophy.core.util

import com.github.warnastrophy.core.data.provider.KeyStoreProvider
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtils {

  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val IV_LENGTH = 12
  private const val TAG_LENGTH = 128

  /**
   * Encrypts the given [plainText] using AES-GCM with a key retrieved from [KeyStoreProvider].
   *
   * A new random IV is generated for each encryption, prepended to the ciphertext, and then encoded
   * in Base64.
   *
   * @param plainText The clear text to encrypt.
   * @return A Base64-encoded string containing the IV and encrypted data.
   * @throws javax.crypto.IllegalBlockSizeException If encryption fails due to invalid input size.
   * @throws javax.crypto.BadPaddingException If encryption fails due to padding issues.
   * @throws java.security.GeneralSecurityException If the cipher initialization fails.
   */
  fun encrypt(plainText: String): String {
    val secretKey: SecretKey = KeyStoreProvider.getOrCreateKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val encrypted = cipher.doFinal(plainText.toByteArray())
    val result = cipher.iv + encrypted
    return Base64.getEncoder().encodeToString(result)
  }

  /**
   * Decrypts a Base64-encoded ciphertext previously encrypted with [encrypt].
   *
   * This method extracts the IV from the first 12 bytes, initializes the cipher, and then performs
   * AES-GCM decryption.
   *
   * @param cipherText The Base64-encoded IV + encrypted data.
   * @return The original plaintext.
   * @throws javax.crypto.AEADBadTagException If the ciphertext or authentication tag is invalid.
   * @throws javax.crypto.BadPaddingException If decryption fails due to tampering or key mismatch.
   * @throws java.security.GeneralSecurityException If the cipher initialization fails.
   */
  fun decrypt(cipherText: String): String {
    val data = Base64.getDecoder().decode(cipherText)
    if (data.size < IV_LENGTH) {
      throw IllegalArgumentException("Invalid data: too short")
    }
    val iv = data.copyOfRange(0, IV_LENGTH)
    val encrypted = data.copyOfRange(IV_LENGTH, data.size)

    val secretKey: SecretKey = KeyStoreProvider.getOrCreateKey()
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val spec = GCMParameterSpec(TAG_LENGTH, iv)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
    val decrypted = cipher.doFinal(encrypted)
    return String(decrypted)
  }
}
