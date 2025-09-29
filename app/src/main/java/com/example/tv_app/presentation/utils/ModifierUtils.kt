package com.example.tv_app.presentation.utils

import android.view.KeyEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent

fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null
) = onPreviewKeyEvent {
    fun onActionUp(block: () -> Unit) {
        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) block()
    }
    when (it.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
            onLeft?.apply { onActionUp(::invoke); return@onPreviewKeyEvent true }
        }
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
            onRight?.apply { onActionUp(::invoke); return@onPreviewKeyEvent true }
        }
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
            onEnter?.apply { onActionUp(::invoke); return@onPreviewKeyEvent true }
        }
    }
    false
}

fun Modifier.handleDPadKeyEvents(
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null,
    onUp: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    onEnter: (() -> Unit)? = null
) = onKeyEvent {
    if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
        when (it.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                onLeft?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                onRight?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                onUp?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                onDown?.invoke().also { return@onKeyEvent true }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onEnter?.invoke().also { return@onKeyEvent true }
            }
        }
    }
    false
}

/**
 * Used to apply modifiers conditionally.
 */
fun Modifier.ifElse(
    condition: () -> Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier
): Modifier = then(if (condition()) ifTrueModifier else ifFalseModifier)

/**
 * Used to apply modifiers conditionally.
 */
fun Modifier.ifElse(
    condition: Boolean,
    ifTrueModifier: Modifier,
    ifFalseModifier: Modifier = Modifier
): Modifier = ifElse({ condition }, ifTrueModifier, ifFalseModifier)
