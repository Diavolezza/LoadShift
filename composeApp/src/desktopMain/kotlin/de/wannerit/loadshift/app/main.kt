package de.wannerit.loadshift.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Desktop
import java.awt.Taskbar
import javax.imageio.ImageIO
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** Desktop app. Options as "-scenario 2 -play -ticks 40 -careful -lang de". */
fun main(args: Array<String>) {
    // The name in the macOS menu bar (otherwise the main class, "MainKt"); must be set before AWT starts.
    System.setProperty("apple.awt.application.name", "Load Shift")
    val pairs = mutableMapOf<String, String?>()
    var i = 0
    while (i < args.size) {
        val key = args[i].removePrefix("-")
        val value = args.getOrNull(i + 1)?.takeUnless { it.startsWith("-") }
        pairs[key] = value
        i += if (value == null) 1 else 2
    }
    // The app's icon in the Dock and the task bar instead of Java's.
    val iconImage = Thread.currentThread().contextClassLoader.getResource("icon.png")?.let { ImageIO.read(it) }
    if (iconImage != null && Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
        Taskbar.getTaskbar().iconImage = iconImage
    }
    val windowIcon = iconImage?.let { BitmapPainter(it.toComposeImageBitmap()) }
    application {
        var about by remember { mutableStateOf(false) }
        // "About Load Shift" in the macOS app menu.
        LaunchedEffect(Unit) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_ABOUT)) {
                Desktop.getDesktop().setAboutHandler { about = true }
            }
        }
        Window(onCloseRequest = ::exitApplication, title = "Load Shift", icon = windowIcon, state = rememberWindowState(width = 1440.dp, height = 960.dp)) {
            App(LaunchOptions.parse(pairs))
        }
        if (about) {
            DialogWindow(
                onCloseRequest = { about = false }, title = t("About Load Shift"), resizable = false,
                state = rememberDialogState(size = DpSize(520.dp, 400.dp)),
            ) {
                AboutContent(Modifier.fillMaxSize().background(Palette.panel))
            }
        }
    }
}
