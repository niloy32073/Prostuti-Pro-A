package com.prostuti.student.data.model.response

import com.prostuti.student.util.LANGUAGE_BANGLA

/** A subject, chapter or topic name in both app languages. A blank [en] falls back to Bangla. */
data class CurriculumName(val bn: String, val en: String) {
    fun inLanguage(language: String): String = if (language == LANGUAGE_BANGLA) bn else en.ifBlank { bn }
}
