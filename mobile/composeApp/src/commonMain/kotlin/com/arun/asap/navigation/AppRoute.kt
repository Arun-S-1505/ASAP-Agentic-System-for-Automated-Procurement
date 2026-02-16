package com.arun.asap.navigation

sealed class AppRoute(val route: String) {
    data object Splash : AppRoute("splash")
    data object Login : AppRoute("login")
    data object Main : AppRoute("main")
    data object DecisionDetail : AppRoute("decision_detail/{erpId}") {
        fun createRoute(erpId: String): String = "decision_detail/$erpId"
        const val ARG_ERP_ID = "erpId"
    }
}
