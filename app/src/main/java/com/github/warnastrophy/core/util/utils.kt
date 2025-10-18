package com.github.warnastrophy.core.util

import java.io.File

fun debugPrint(msg: String) {
  val file = File("debug.txt")
  file.appendText(msg + "\n")
}
