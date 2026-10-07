package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.admin.AdminApp
import com.example.ui.theme.MyApplicationTheme
import com.example.user.UserApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var activeAppMode by remember { mutableStateOf("USER") } // USER or ADMIN

            MyApplicationTheme(isAdmin = activeAppMode == "ADMIN") {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (activeAppMode == "ADMIN") {
                        AdminApp(
                            onSwitchToUserApp = { activeAppMode = "USER" }
                        )
                    } else {
                        UserApp(
                            onLaunchAdminApp = {
                                activeAppMode = "ADMIN"
                            }
                        )
                    }
                }
            }
        }
    }
}
