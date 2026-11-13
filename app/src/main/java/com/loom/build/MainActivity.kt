package com.loom.build

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.loom.build.data.ApkInstaller
import com.loom.build.data.BuildStore
import com.loom.build.feature.BindScreen
import com.loom.build.feature.BuildScreen
import com.loom.build.feature.ProjectScreen
import com.loom.build.feature.SettingsScreen
import com.loom.build.ui.BottomNav
import com.loom.build.ui.theme.LoomBuild
import com.loom.build.ui.theme.LoomBuildTheme

class MainActivity : ComponentActivity() {

    private lateinit var store: BuildStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = BuildStore(applicationContext)
        setContent {
            LoomBuildTheme {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(LoomBuild.colors.paper),
                ) {
                    Root(store)
                }
            }
        }
    }
}

@Composable
private fun Root(store: BuildStore) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val openUrl: (String) -> Unit = remember {
        { url: String -> ApkInstaller.openUrl(context, url) }
    }

    if (!store.bound) {
        BindScreen(store, openUrl)
        return
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> ProjectScreen(store) { tab = 1 }
                1 -> BuildScreen(store, openUrl)
                else -> SettingsScreen(store, openUrl)
            }
        }
        BottomNav(tab) { tab = it }
    }
}
