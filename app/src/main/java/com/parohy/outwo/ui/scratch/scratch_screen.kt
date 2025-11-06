package com.parohy.outwo.ui.scratch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.parohy.outwo.scratch.core.Content
import com.parohy.outwo.scratch.core.Failure
import com.parohy.outwo.scratch.core.Loading
import com.parohy.outwo.ui.ScreenError
import com.parohy.outwo.ui.ScreenLoading
import com.parohy.outwo.ui.alertDialog
import com.parohy.outwo.ui.cards.CardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScratchScreenComposable(goToActivation: (String) -> Unit) {
  Scaffold(
    topBar  = {
      TopAppBar(
        title = { Text("Scratch card") },
      )
    },
    content = { contentPadding ->
      val viewModel: ScratchViewModel = hiltViewModel()
      val uiState = viewModel.uiState.collectAsState()

      Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        when (val card = uiState.value.card) {
          is Content -> CardItem(card = card.value, onClick = {
            if (card.value.isScratched) {
              goToActivation(card.value.code)
            } else
              viewModel.scratchCard()
          })
          is Failure -> ScreenError(card.value.message)
          is Loading -> ScreenLoading()
          null -> {/*do nothing*/}
        }

        val failedToScratchAlert = alertDialog(onDismiss = viewModel::clearScratchState)

        when (val scratch = uiState.value.scratch) {
          is Content -> {/*do nothing*/}
          is Failure -> failedToScratchAlert("Failed to scratch", scratch.value.message ?: "Unknown error")
          is Loading -> ScreenLoading(0.7f)
          null -> {/*do nothing*/}
        }
      }
    }
  )
}
