package com.example.fraudlens.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun NavRoutes(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.login.route) {
        composable(Screen.login.route){

        }
    }
}