package com.github.warnastrophy.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsSubsequenceUtilTest {

  @Test
  fun `empty list is subsequence of empty list`() {
    assertTrue(emptyList<Int>().isSubsequenceOf(emptyList()))
  }

  @Test
  fun `empty list is subsequence of non empty list`() {
    assertTrue(emptyList<Int>().isSubsequenceOf(listOf(1, 2, 3)))
  }

  @Test
  fun `identical lists are subsequences`() {
    val a = listOf(1, 2, 3)
    assertTrue(a.isSubsequenceOf(a))
  }

  @Test
  fun `non consecutive elements in order are subsequence`() {
    val subseq = listOf(1, 3)
    val full = listOf(1, 2, 3)
    assertTrue(subseq.isSubsequenceOf(full))
  }

  @Test
  fun `same elements but wrong order is not subsequence`() {
    val subseq = listOf(2, 1)
    val full = listOf(1, 2, 3)
    assertFalse(subseq.isSubsequenceOf(full))
  }

  @Test
  fun `element not present is not subsequence`() {
    assertFalse(listOf(4).isSubsequenceOf(listOf(1, 2, 3)))
  }

  @Test
  fun `repeated elements - enough occurrences available`() {
    val subseq = listOf(1, 1, 2)
    val full = listOf(1, 1, 2, 3)
    assertTrue(subseq.isSubsequenceOf(full))
  }

  @Test
  fun `repeated elements - not enough occurrences available`() {
    val subseq = listOf(1, 1, 2)
    val full = listOf(1, 2)
    assertFalse(subseq.isSubsequenceOf(full))
  }

  @Test
  fun `string subsequence preserves order`() {
    val subseq = listOf("a", "c")
    val full = listOf("a", "b", "c", "d")
    assertTrue(subseq.isSubsequenceOf(full))
  }
}
