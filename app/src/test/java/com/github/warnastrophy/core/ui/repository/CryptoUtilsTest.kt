package com.github.warnastrophy.core.ui.repository

import com.github.warnastrophy.core.model.util.CryptoUtils
import com.github.warnastrophy.core.model.util.KeyStoreProvider
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CryptoUtilsTest {
  @Before
  fun setUp() {
    KeyStoreProvider.getOrCreateKey()
  }

  @Test
  fun `encrypt then decrypt returns original text`() {
    val plaintext = "test string"
    val encrypted = CryptoUtils.encrypt(plaintext)
    val decrypted = CryptoUtils.decrypt(encrypted)
    assertEquals(plaintext, decrypted)
  }

  @Test
  fun `decrypt with invalid base64 throws exception`() {
    assertThrows(IllegalArgumentException::class.java) {
      CryptoUtils.decrypt("$$$\\invalid_base64$$$")
    }
  }

  @Test
  fun `decrypt with too short data throws exception`() {
    // base64 of a string shorter than IV length (12 bytes)
    val tooShort = java.util.Base64.getEncoder().encodeToString(ByteArray(8))
    assertThrows(IllegalArgumentException::class.java) { CryptoUtils.decrypt(tooShort) }
  }
}
