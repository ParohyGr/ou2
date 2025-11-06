@file:OptIn(ExperimentalCoroutinesApi::class)

package com.parohy.outwo.scratch.repo

import app.cash.turbine.test
import com.parohy.outwo.scratch.core.*
import com.parohy.outwo.scratch.database.CardsDao
import com.parohy.outwo.scratch.network.ApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val SUCCESS_CODE = 280000
private const val FAILURE_CODE = 270000

class CardRepositoryTest {
  @Mock
  private lateinit var db: CardsDao
  private lateinit var cardsRepository: CardsRepository
  private lateinit var apiService: ApiService

  private val dispatcher = StandardTestDispatcher()

  private lateinit var mockWebServer: MockWebServer

  @Before
  fun setup() {
    MockitoAnnotations.initMocks(this)

    mockWebServer = MockWebServer()
    mockWebServer.start()

    apiService = Retrofit.Builder()
      .baseUrl(mockWebServer.url("/"))
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(ApiService::class.java)

    cardsRepository = CardsRepositoryImpl(db, apiService, TestScope(dispatcher))

    Dispatchers.setMain(dispatcher)
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
    if (::mockWebServer.isInitialized)
      mockWebServer.shutdown()
  }

  @Test
  fun `initial state`() = runTest {
    assertEquals(cardsRepository.getData().first(), CardRepositoryState())
  }

  @Test
  fun `when loadCards, state should update`() = runTest {
    val results = mutableListOf<CardRepositoryState>()
    val job = launch {
      cardsRepository.getData().toList(results)
    }

    cardsRepository.loadCards()
    advanceUntilIdle()

    job.cancel()

    assertTrue(results.last().cards.isContent)
  }

  @Test
  fun `when generateCard, state should update`() = runTest {
    val results = mutableListOf<CardRepositoryState>()
    val job = launch {
      cardsRepository.getData().toList(results)
    }

    cardsRepository.generateCard()
    advanceUntilIdle()

    job.cancel()

    assertTrue(results.last().cards.isContent)
  }

  @Test
  fun `when scratchCard, state should update`() = runTest {
    val results = mutableListOf<CardRepositoryState>()
    val job = launch {
      cardsRepository.getData().toList(results)
    }

    cardsRepository.generateCard()
    cardsRepository.scratchCard(cardsRepository.getData().first().cards.valueOrNull?.keys?.first() ?: "123")
    advanceUntilIdle()

    job.cancel()

    assertTrue("Expected scratched card but ${results.last().cards.valueOrNull?.values?.lastOrNull()?.isScratched}", results.last().cards.valueOrNull?.values?.lastOrNull()?.isScratched ?: false)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `when scratchCard with not existing card, function should throw`() = runTest {
    cardsRepository.scratchCard("123")
  }

  @Test
  fun `when removeCard, state should update`() = runTest {
    val results = mutableListOf<CardRepositoryState>()
    val job = launch {
      cardsRepository.getData().toList(results)
    }

    cardsRepository.generateCard()
    cardsRepository.removeCard(cardsRepository.getData().first().cards.valueOrNull?.keys?.first() ?: "123")
    advanceUntilIdle()

    job.cancel()

    assertTrue("Expected empty cards but ${results.last().cards.valueOrNull?.values}", results.last().cards.valueOrNull?.values?.isEmpty() ?: false)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `when removeCard with not existing card, function should throw`() = runTest {
    cardsRepository.removeCard("123")
  }

  @Test
  fun `given correct result, when activateCard, state should update Content`() = runTest {
    val response = MockResponse().setResponseCode(200).setBody("{ \"android\": \"$SUCCESS_CODE\" }")
    mockWebServer.enqueue(response)

    cardsRepository = CardsRepositoryImpl(db, apiService, TestScope(dispatcher))

    cardsRepository.getData().test {
      awaitItem() //consume initial state
      cardsRepository.generateCard()
      val i1 = awaitItem()
      assertNull(i1.activation)
      cardsRepository.activateCard(i1.cards.valueOrNull!!.keys.first())
      val i2 = awaitItem()
      assertTrue(i2.activation.isLoading)
      val i3 = awaitItem()
      assertTrue("Expected Failure but have ${i3.activation.valueOrNull}", i3.activation.isContent)
    }
  }

  @Test
  fun `given failure result, when activateCard, state should update Failure`() = runTest {
    val response = MockResponse().setResponseCode(200).setBody("{ \"android\": \"$FAILURE_CODE\" }")
    mockWebServer.enqueue(response)

    cardsRepository = CardsRepositoryImpl(db, apiService, TestScope(dispatcher))

    cardsRepository.getData().test {
      awaitItem() //consume initial state
      cardsRepository.generateCard()
      val i1 = awaitItem()
      assertNull(i1.activation)
      cardsRepository.activateCard(i1.cards.valueOrNull!!.keys.first())
      val i2 = awaitItem()
      assertTrue(i2.activation.isLoading)
      val i3 = awaitItem()
      assertTrue("Expected Failure but have ${i3.activation.valueOrNull}", i3.activation.isFailure)
    }
  }
}