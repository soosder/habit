package de.soosser.dopamineparadise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.enableEdgeToEdge
import de.soosser.dopamineparadise.ui.DopamineApp
import de.soosser.dopamineparadise.ui.theme.DopamineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DopamineTheme {
                DopamineApp()
            }
        }
    }
}
