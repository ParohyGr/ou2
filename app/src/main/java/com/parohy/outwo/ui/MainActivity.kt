package com.parohy.outwo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import com.parohy.outwo.ui.activate.ActivationScreenComposable
import com.parohy.outwo.ui.scratch.ScratchScreenComposable
import com.parohy.outwo.ui.theme.O2ScratchTheme
import com.parohy.outwo.ui.cards.CardsScreenComposable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

const val NAV_ARG_CARD_CODE: String = "card_code_arg"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val navController = rememberNavController()

      O2ScratchTheme {
        NavHost(navController = navController, startDestination = "cards") {
          composable(
            route = "cards"
          ) {
            CardsScreenComposable { card ->
              navController.navigate(
                route = if (card.isScratched)
                  "activate/${card.code}"
                else
                  "scratch/${card.code}"
              )
            }
          }

          composable(
            route = "activate/{$NAV_ARG_CARD_CODE}",
            arguments = listOf(navArgument(NAV_ARG_CARD_CODE) { defaultValue = "" })
          ) {
            ActivationScreenComposable()
          }

          composable(
            route = "scratch/{$NAV_ARG_CARD_CODE}",
            arguments = listOf(navArgument(NAV_ARG_CARD_CODE) { defaultValue = "" })
          ) {
            ScratchScreenComposable { code ->
              navController.popBackStack()
              navController.navigate("activate/$code")
            }
          }
        }
      }
    }
  }
}