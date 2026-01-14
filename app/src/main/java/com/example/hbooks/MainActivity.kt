package com.example.hbooks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hbooks.data.repository.ThemeMode
import com.example.hbooks.data.repository.ThemeRepository
import com.example.hbooks.services.PlaybackStateManager
import com.example.hbooks.ui.components.MiniPlayer
import com.example.hbooks.ui.navigation.AppNavigation
import com.example.hbooks.ui.navigation.BottomNavigationBar
import com.example.hbooks.ui.theme.HBooksTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize PlaybackStateManager
        PlaybackStateManager.initialize(this)
        
        // Initialize ThemeRepository
        ThemeRepository.initialize(this)

        enableEdgeToEdge()
        setContent {
            // Observe theme mode
            val themeMode by ThemeRepository.themeMode.collectAsStateWithLifecycle()
            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemDark
            }
            
            HBooksTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val bottomBarRoutes = setOf("home", "search", "library", "profile")
                val showBottomBar = currentRoute in bottomBarRoutes
                
                // Get playback state for MiniPlayer
                val playbackState by PlaybackStateManager.playbackState.collectAsStateWithLifecycle()
                val isOnPlayerScreen = currentRoute?.startsWith("player") == true
                val showMiniPlayer = playbackState.currentBook != null && !isOnPlayerScreen

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Column {
                            // Show MiniPlayer above bottom nav when playing and not on player screen
                            if (showMiniPlayer) {
                                MiniPlayer(
                                    playbackState = playbackState,
                                    onPlayerClick = {
                                        playbackState.currentBook?.let { book ->
                                            navController.navigate("player/${book.id}")
                                        }
                                    },
                                    onPlayPauseClick = {
                                        PlaybackStateManager.togglePlayPause()
                                    }
                                )
                            }
                            if (showBottomBar) {
                                BottomNavigationBar(navController = navController)
                            }
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(navController = navController, modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
