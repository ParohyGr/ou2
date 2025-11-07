package com.parohy.outwo.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.scratch.repo.*
import com.parohy.outwo.ui.cards.CardsViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var cardsRepository: CardsRepository
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var viewModel: CardsViewModel

  private val repoDataFlow = MutableStateFlow(CardRepositoryState(cards = Content(emptyMap())))

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    cardsRepository = mockk(relaxed = true)
    every { cardsRepository.getData() } returns repoDataFlow

    savedStateHandle = mockk(relaxed = true)
    every { savedStateHandle.get<String>("cards") } returns null

    viewModel = CardsViewModel(savedStateHandle, cardsRepository, testScope)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init block should observe repository data and update uiState`() = runTest {
    val card1 = ScratchCard("123", true, false)
    val card2 = ScratchCard("456", false, true)
    val repoCards = mapOf("123" to card1, "456" to card2)

    viewModel.uiState.test {
      val i = awaitItem()
      assertTrue(i.cards is Content)
      repoDataFlow.emit(CardRepositoryState(cards = Content(repoCards)))
      val expectedContent = listOf(card1, card2)
      val updatedState = awaitItem()
      assertTrue("Cards state should be Content but was ${updatedState.cards}", updatedState.cards is Content)
      val actualCards = (updatedState.cards as Content).value
      assertEquals(expectedContent, actualCards)
    }

    val expectedSavedString = "123;true;false-456;false;true"
    coVerify { savedStateHandle["cards"] = expectedSavedString }
  }


  @Test
  fun `generateCard should emit Loading then Content`() = runTest {
    coEvery { cardsRepository.generateCard() } returns Unit

    viewModel.uiState.test {
      awaitItem() // initial
      viewModel.generateCard()
      val loadingState = awaitItem()
      assertTrue("Generate state should be Loading but was ${loadingState.generate}", loadingState.generate is Loading)
      val successState = awaitItem()
      assertTrue("Generate state should be Content but was ${successState.generate}", successState.generate is Content)
      coVerify(exactly = 1) { cardsRepository.generateCard() }
    }
  }

  @Test
  fun `generateCard should emit Loading then Failure on exception`() = runTest {
    val testException = RuntimeException("Network Error")
    coEvery { cardsRepository.generateCard() } throws testException

    viewModel.uiState.test {
      awaitItem() // initial
      viewModel.generateCard()
      val loadingState = awaitItem()
      assertTrue("Generate state should be Loading but was ${loadingState.generate}", loadingState.generate is Loading)
      val failureState = awaitItem()
      assertTrue("Generate state should be Failure but was ${failureState.generate}", failureState.generate is Failure<*>)
      val failure = failureState.generate as Failure<Throwable>
      assertTrue(
        "Wrapped exception message should contain the original message",
        (failure.value as RuntimeException).cause == testException
      )

      coVerify(exactly = 1) { cardsRepository.generateCard() }
    }
  }

  @Test
  fun `clearGenerateState should reset generate state to null`() = runTest {
    coEvery { cardsRepository.generateCard() } returns Unit
    viewModel.uiState.test {
      awaitItem() // Initial
      viewModel.generateCard()
      val loadingState = awaitItem()
      assertTrue("Generate state should be Loading but was ${loadingState.generate}", loadingState.generate is Loading)
      val successState = awaitItem()
      assertTrue(successState.generate is Content)

      viewModel.clearGenerateState()
      val clearedState = awaitItem()
      assertEquals(null, clearedState.generate)
    }
  }
}