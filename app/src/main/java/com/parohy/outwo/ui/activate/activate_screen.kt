package com.parohy.outwo.ui.activate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.ui.ScreenError
import com.parohy.outwo.ui.ScreenLoading
import com.parohy.outwo.ui.alertDialog
import com.parohy.outwo.ui.cards.CardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreenComposable() {
  val snackBar = remember { SnackbarHostState() }

  Scaffold(
    topBar  = {
      TopAppBar(
        title = { Text("Activate card") },
      )
    },
    content = { contentPadding ->
      val viewModel: ActivateViewModel = hiltViewModel()
      val uiState = viewModel.uiState.collectAsState()

      Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        when (val card = uiState.value.card) {
          is Content -> CardItem(card = card.value, onClick = { viewModel.activateCard() })
          is Failure -> ScreenError(card.value.message)
          is Loading -> ScreenLoading()
          null       -> {/*do nothing*/}
        }

        val alertDialog = alertDialog(onDismiss = { viewModel.clearActivationState() })

        LaunchedEffect(uiState.value.activate) {
          when (val activation = uiState.value.activate) {
            is Content -> snackBar.showSnackbar(message = "Card activated", duration = SnackbarDuration.Long)
            is Failure -> alertDialog("Failed to activate", activation.value.message ?: "Unknown error")
            else       -> {/*do nothing*/}
          }
        }

        if (uiState.value.activate.isLoading)
          ScreenLoading(0.7f)
      }
    },
    snackbarHost = { SnackbarHost(hostState = snackBar) }
  )
}