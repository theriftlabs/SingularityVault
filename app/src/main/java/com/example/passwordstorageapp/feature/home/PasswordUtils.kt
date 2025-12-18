package com.example.passwordstorageapp.feature.home

import kotlin.random.Random

data class PasswordStrengthResult(
    val isStrong: Boolean,
    val issues: List<String>
)

fun checkPasswordStrength(password: String): PasswordStrengthResult {
    val issues = mutableListOf<String>()

    if (password.length < 12) {
        issues.add("At least 12 characters")
    }
    if (!password.any { it.isUpperCase() }) {
        issues.add("Include an uppercase letter")
    }
    if (!password.any { it.isLowerCase() }) {
        issues.add("Include a lowercase letter")
    }
    if (!password.any { it.isDigit() }) {
        issues.add("Include a number")
    }
    if (!password.any { "!@#\$%^&*()-_=+[]{};:,.<>?".contains(it) }) {
        issues.add("Include a symbol")
    }

    return PasswordStrengthResult(
        isStrong = issues.isEmpty(),
        issues = issues
    )
}

fun generateStrongPassword(length: Int = 16): String {
    val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val lower = "abcdefghijklmnopqrstuvwxyz"
    val digits = "0123456789"
    val symbols = "!@#\$%^&*()-_=+[]{};:,.<>?"

    val all = upper + lower + digits + symbols

    // guarantee complexity
    val required = listOf(
        upper.random(),
        lower.random(),
        digits.random(),
        symbols.random()
    )

    val remaining = (length - required.size).coerceAtLeast(0)
        .let { count ->
            (1..count).map { all.random() }
        }

    return (required + remaining)
        .shuffled(Random(System.nanoTime()))
        .joinToString("")
}