package com.github.warnastrophy.core.util

import com.github.warnastrophy.core.data.provider.KeyStoreProvider
import java.util.Base64
import junit.framework.TestCase
import org.junit.Assert
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
    TestCase.assertEquals(plaintext, decrypted)
  }

  @Test
  fun `decrypt with invalid base64 throws exception`() {
    Assert.assertThrows(IllegalArgumentException::class.java) {
      CryptoUtils.decrypt("$$$\\invalid_base64$$$")
    }
  }

  @Test
  fun `decrypt with too short data throws exception`() {
    // base64 of a string shorter than IV length (12 bytes)
    val tooShort = Base64.getEncoder().encodeToString(ByteArray(8))
    Assert.assertThrows(IllegalArgumentException::class.java) { CryptoUtils.decrypt(tooShort) }
  }
}
