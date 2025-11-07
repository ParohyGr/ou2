package com.parohy.outwo.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.parohy.outwo.scratch.core.Content
import com.parohy.outwo.scratch.core.Failure
import com.parohy.outwo.scratch.core.Loading
import com.parohy.outwo.scratch.repo.*
import com.parohy.outwo.ui.NAV_ARG_CARD_CODE
import com.parohy.outwo.ui.activate.ActivateViewModel
import com.parohy.outwo.ui.scratch.ScratchViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue


@OptIn(ExperimentalCoroutinesApi::class)
class ScratchViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var cardsRepository: CardsRepository
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var viewModel: ScratchViewModel

  private val testCardCode = "CARD-5678"
  private val mockCard = ScratchCard(testCardCode, isScratched = false, isActivated = false)
  private val mockCardMap = mapOf(testCardCode to mockCard)

  private val repoDataFlow = MutableStateFlow(CardRepositoryState(cards = Content(mockCardMap)))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    cardsRepository = mockk(relaxed = true)
    every { cardsRepository.getData() } returns repoDataFlow

    savedStateHandle = mockk(relaxed = true)
    every { savedStateHandle.get<String>(NAV_ARG_CARD_CODE) } returns testCardCode
    every { savedStateHandle.get<String>("card") } returns null

    repoDataFlow.value = repoDataFlow.value.copy(cards = Content(mockCardMap))

    viewModel = ScratchViewModel(savedStateHandle, cardsRepository, testScope)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init should populate card state correctly`() = runTest {
    viewModel.uiState.test {
      awaitItem()
      val initial = awaitItem()

      assertTrue(initial.card is Content)
      assertEquals(mockCard, (initial.card as Content).value)
      assertEquals(null, initial.scratch)
    }
  }

  @Test
  fun `scratchCard should emit Loading then Content on success AND update repo data`() = runTest {
    val scratchedCard = mockCard.copy(isScratched = true)
    val updatedCardMap = mockCardMap + (testCardCode to scratchedCard)

    coEvery { cardsRepository.scratchCard(testCardCode) } coAnswers {
      repoDataFlow.value = repoDataFlow.value.copy(cards = Content(updatedCardMap))
    }

    viewModel.uiState.test {
      awaitItem() // Initial

      viewModel.scratchCard()

      val loadingState = awaitItem()
      assertTrue("Scratch state should be Loading but was ${loadingState.scratch}", loadingState.scratch is Loading)

      val successState = awaitItem()
      assertTrue("Scratch state should be Content but was ${successState.scratch}", successState.scratch is Content)
      awaitItem()

      val updatedState = awaitItem()
      assertTrue("Card should be scratched but was ${updatedState.card}", (updatedState.card as? Content)?.value?.isScratched == true)
    }
    coVerify(exactly = 1) { cardsRepository.scratchCard(testCardCode) }
  }

  @Test
  fun `scratchCard should emit Loading then Failure on exception`() = runTest {
    val testException = IllegalStateException("Card is already scratched")

    coEvery { cardsRepository.scratchCard(testCardCode) } throws testException

    viewModel.uiState.test {
      awaitItem() // Initial

      viewModel.scratchCard()

      val loadingState = awaitItem()
      assertTrue("Scratch state should be Loading but was ${loadingState.scratch}", loadingState.scratch is Loading)

      val failureState = awaitItem()
      assertTrue("Scratch state should be Failure but was ${failureState.scratch}", failureState.scratch is Failure)

      assertEquals(testException.message, (failureState.scratch as Failure<Throwable>).value.message)

      val updatedState = awaitItem()
      assertTrue("Card should not be scratched but was ${updatedState.card}", (updatedState.card as? Content)?.value?.isScratched == false)
    }
    coVerify(exactly = 1) { cardsRepository.scratchCard(testCardCode) }
  }

  @Test
  fun `clearScratchState should reset scratch state to null`() = runTest {
    coEvery { cardsRepository.scratchCard(testCardCode) } coAnswers {
      repoDataFlow.value = repoDataFlow.value.copy(cards = Content(mapOf(testCardCode to mockCard.copy(isScratched = true))))
    }

    viewModel.uiState.test {
      awaitItem() // Initial

      viewModel.scratchCard()
      awaitItem() // Loading
      awaitItem() // Content

      viewModel.clearScratchState()
      awaitItem()

      val clearedState = awaitItem()
      assertEquals(null, clearedState.scratch)
    }
  }
}