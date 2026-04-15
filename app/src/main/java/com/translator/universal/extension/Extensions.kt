package com.translator.universal.extension

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

// View Extensions
fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.onClick(action: () -> Unit) {
    setOnClickListener { action() }
}

// Context Extensions
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.toastLong(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Fragment.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().toast(message, duration)
}

fun Fragment.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    view?.let {
        Snackbar.make(it, message, duration).show()
    }
}

fun Fragment.showSnackbarWithAction(
    message: String,
    actionText: String,
    action: () -> Unit
) {
    view?.let {
        Snackbar.make(it, message, Snackbar.LENGTH_LONG)
            .setAction(actionText) { action() }
            .show()
    }
}

// String Extensions
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.isValidUrl(): Boolean {
    return android.util.Patterns.WEB_URL.matcher(this).matches()
}

fun String.extractUrls(): List<String> {
    val urlPattern = android.util.Patterns.WEB_URL
    val matcher = urlPattern.matcher(this)
    val urls = mutableListOf<String>()
    while (matcher.find()) {
        urls.add(matcher.group())
    }
    return urls
}

fun String.toSlug(): String {
    return lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim()
}

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (length > maxLength) {
        substring(0, maxLength - suffix.length) + suffix
    } else this
}

// Collection Extensions
fun <T> List<T>.takeRandom(count: Int): List<T> {
    return if (size <= count) this else shuffled().take(count)
}

fun <T> List<T>.safeSublist(start: Int, end: Int): List<T> {
    val safeEnd = minOf(end, size)
    val safeStart = minOf(start, safeEnd)
    return if (safeStart < safeEnd) subList(safeStart, safeEnd) else emptyList()
}

// Date/Time Extensions
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Az önce"
        diff < 3600_000 -> "${diff / 60_000} dakika önce"
        diff < 86400_000 -> "${diff / 3600_000} saat önce"
        diff < 604800_000 -> "${diff / 86400_000} gün önce"
        else -> "${diff / 604800_000} hafta önce"
    }
}

// Number Extensions
fun Int.toFormattedString(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> toString()
    }
}

// Result Extensions
inline fun <T> runCatching(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}