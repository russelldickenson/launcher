package org.fossify.home.extensions

import java.text.Normalizer

private val diacriticsRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun String.normalizeString() = Normalizer.normalize(this, Normalizer.Form.NFD).replace(diacriticsRegex, "")
