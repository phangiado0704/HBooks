package com.example.hbooks.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.hbooks.ui.screens.ForgotPasswordScreen
import com.example.hbooks.ui.screens.HomeScreen
import com.example.hbooks.ui.screens.LibraryScreen
import com.example.hbooks.ui.screens.LoginScreen
import com.example.hbooks.ui.screens.PlayerScreen
import com.example.hbooks.ui.screens.ProfileScreen
import com.example.hbooks.ui.screens.RegisterScreen
import com.example.hbooks.ui.screens.SearchScreen

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "login", modifier = modifier) {
        composable("login") {
            LoginScreen(
                onSignIn = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate("register") },
                onForgotPassword = { navController.navigate("forgotPassword") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegister = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onSignInClick = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("forgotPassword") {
            ForgotPasswordScreen(
                onEmailSent = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackClick = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(onBookClick = { bookId ->
                navController.navigate("player/$bookId")
            })
        }
        composable("player/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId")
            PlayerScreen(
                bookId = bookId,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("search") {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("player/$bookId") }
            )
        }
        composable("library") {
            LibraryScreen(
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId -> navController.navigate("player/$bookId") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
