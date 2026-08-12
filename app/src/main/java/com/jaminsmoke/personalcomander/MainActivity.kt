package com.jaminsmoke.personalcomander

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.jaminsmoke.personalcomander.ui.PersonalComanderApp
import com.jaminsmoke.personalcomander.ui.theme.PersonalComanderTheme
import com.jaminsmoke.personalcomander.ui.theme.PcBackground
import com.jaminsmoke.personalcomander.ui.theme.PcSurfaceContainerLowest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkScrim = PcBackground.toArgb()
        val navScrim = PcSurfaceContainerLowest.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(darkScrim),
            navigationBarStyle = SystemBarStyle.dark(navScrim),
        )
        setContent {
            PersonalComanderTheme {
                PersonalComanderApp()
            }
        }
    }
}
