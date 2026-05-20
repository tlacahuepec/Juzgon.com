package com.juzgon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.juzgon.navigation.JuzgonApp
import com.juzgon.ui.theme.JuzgonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuzgonTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JuzgonApp()
                }
            }
        }
    }
}
