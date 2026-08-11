package com.jaminsmoke.personalcomander

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jaminsmoke.personalcomander.ui.PersonalComanderApp
import com.jaminsmoke.personalcomander.ui.theme.PersonalComanderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalComanderTheme {
                PersonalComanderApp()
            }
        }
    }
}
