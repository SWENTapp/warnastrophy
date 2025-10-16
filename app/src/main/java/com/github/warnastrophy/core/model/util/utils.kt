package com.github.warnastrophy.core.model.util

fun debugPrint(msg: String) {
  val file = java.io.File("debug.txt")
  file.appendText(msg + "\n")
}
