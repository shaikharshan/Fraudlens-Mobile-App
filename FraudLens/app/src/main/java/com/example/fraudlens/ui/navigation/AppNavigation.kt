package com.example.fraudlens.ui.navigation

sealed class Screen(val route: String){
    object root: Screen("root")
    object login : Screen("login")
    object signup: Screen("signup")
    object biometricCheck: Screen("biometric")
    object createAccount: Screen("createAccount")
    object sendMoney:Screen("sendMoney")
    object locationIP:Screen("locationIP")
    object home: Screen("home")
    object profile: Screen("profile")
    object sendMoney2: Screen("sendMoney2")
    object smsFraudCheck: Screen("smsFraudCheck")
    object liveDetection: Screen("liveDetection")

}