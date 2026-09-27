package com.shubhamthorat.flint.core

/**
 * Lightweight multiplatform logging utility for Flint AI Content Operating System.
 * Formats structured logs across Android, iOS, Desktop, and Web runtime logs.
 */
object FlintLogger {

    fun d(tag: String, message: String) {
        println("🔵 [FLINT_DEBUG][$tag] $message")
    }

    fun i(tag: String, message: String) {
        println("ℹ️ [FLINT_INFO][$tag] $message")
    }

    fun w(tag: String, message: String) {
        println("⚠️ [FLINT_WARN][$tag] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val err = throwable?.message?.let { " | Error: $it" } ?: ""
        println("🔴 [FLINT_ERROR][$tag] $message$err")
    }
}
