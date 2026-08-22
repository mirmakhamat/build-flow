package uz.buildflow.app.core.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Raqamli summalar uchun vizual maska.
 * Masalan: "150000000" -> "150 000 000"
 */
class NumberAmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formattedBuilder = StringBuilder()
        val originalToTransformed = IntArray(original.length + 1)
        val transformedToOriginal = mutableListOf<Int>()

        val len = original.length
        var transformedIndex = 0

        for (i in 0 until len) {
            originalToTransformed[i] = transformedIndex
            formattedBuilder.append(original[i])
            transformedToOriginal.add(i)
            transformedIndex++

            // Oxiridan 3 xonali guruhlash
            val digitsFromEnd = len - (i + 1)
            if (digitsFromEnd > 0 && digitsFromEnd % 3 == 0) {
                formattedBuilder.append(' ')
                transformedToOriginal.add(i + 1)
                transformedIndex++
            }
        }
        originalToTransformed[len] = transformedIndex
        transformedToOriginal.add(len)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, original.length)
                return originalToTransformed[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformedToOriginal.size - 1)
                return transformedToOriginal[clamped]
            }
        }

        return TransformedText(AnnotatedString(formattedBuilder.toString()), offsetMapping)
    }
}

/**
 * Telefon raqami uchun vizual maska.
 * Masalan: "901234567" -> "+998 (90) 123-45-67"
 */
class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 9) text.text.substring(0, 9) else text.text
        var out = if (trimmed.isNotEmpty()) "+998 " else ""

        for (i in trimmed.indices) {
            if (i == 0) out += "("
            out += trimmed[i]
            if (i == 1) out += ") "
            if (i == 4 || i == 6) out += "-"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 2) return offset + 6 // "+998 ("
                if (offset <= 5) return offset + 8 // "+998 (XX) "
                if (offset <= 7) return offset + 9 // "+998 (XX) XXX-"
                if (offset <= 9) return offset + 10 // "+998 (XX) XXX-XX-"
                return out.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 6) return 0
                if (offset <= 8) return offset - 6
                if (offset <= 12) return offset - 8
                if (offset <= 15) return offset - 9
                if (offset <= 18) return offset - 10
                return trimmed.length
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
