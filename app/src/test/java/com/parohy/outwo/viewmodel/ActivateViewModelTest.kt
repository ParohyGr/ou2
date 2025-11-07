package com.parohy.outwo.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.parohy.outwo.scratch.core.Content
import com.parohy.outwo.scratch.core.Failure
import com.parohy.outwo.scratch.repo.*
import com.parohy.outwo.ui.NAV_ARG_CARD_CODE
import com.parohy.outwo.ui.activate.ActivateViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActivateViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var cardsRepository: CardsRepository
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var viewModel: ActivateViewModel

  private val testCardCode = "1234"
  private val mockCard = ScratchCard(testCardCode, isScratched = true, isActivated = false)
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

    viewModel = ActivateViewModel(savedStateHandle, cardsRepository, testScope)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `when activate card is called, and activation succeeds, uiState activate should contain Content state`() = runTest {
    every { cardsRepository.activateCard(any()) } answers {
      repoDataFlow.value = repoDataFlow.value.copy(activation = Content(Unit))
    }

    viewModel.uiState.test {
      awaitItem() // initial
      val updated = awaitItem()
      assertTrue("Card should be Content but was ${updated.card}", updated.card is Content)

      viewModel.activateCard()
      val activateState = awaitItem()

      assertTrue("Card should be Content but was ${activateState.card}", activateState.card is Content)
      assertTrue("Activation should be Content but was ${activateState.activate}", activateState.activate is Content)
    }
  }

  @Test
  fun `when activate card is called, and activation fails, uiState activate should contain Failure state`() = runTest {
    every { cardsRepository.activateCard(any()) } answers {
      repoDataFlow.value = repoDataFlow.value.copy(activation = Failure(RuntimeException("Network Error")))
    }

    viewModel.uiState.test {
      awaitItem() // initial
      val updated = awaitItem()
      assertTrue("Card should be Content but was ${updated.card}", updated.card is Content)

      viewModel.activateCard()
      val activateState = awaitItem()

      assertTrue("Card should be Content but was ${activateState.card}", activateState.card is Content)
      assertTrue("Activation should be Content but was ${activateState.activate}", activateState.activate is Failure)
    }
  }

  @Test
  fun `activateCard should call repository's activateCard`() = runTest {
    viewModel.activateCard()

    verify(exactly = 1) { cardsRepository.activateCard(testCardCode) }
  }

  @Test
  fun `clearActivationState should call repository's resetActivate`() = runTest {
    viewModel.clearActivationState()

    verify(exactly = 1) { cardsRepository.resetActivate() }
  }
}