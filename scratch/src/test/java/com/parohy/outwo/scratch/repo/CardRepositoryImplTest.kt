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
import org.mockito.kotlin.whenever
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
    whenever(db.loadCards()).thenReturn(emptyList())

    cardsRepository.loadCards()
    cardsRepository.getData().test {
      val i = awaitItem()
      assertTrue(i.cards.isContent)
    }
  }

  @Test
  fun `when generateCard, state should update`() = runTest {
    cardsRepository.generateCard()
    cardsRepository.getData().test {
      val i = awaitItem()
      assertTrue(i.cards.isContent)
      assertEquals(1, i.cards.valueOrNull?.size)
    }
  }

  @Test
  fun `when scratchCard, state should update`() = runTest {
    cardsRepository.generateCard()
    cardsRepository.getData().test {
      val i1 = awaitItem()
      assertTrue("Card should not be scratched", i1.cards.valueOrNull?.values?.first()?.isScratched == false)
      cardsRepository.scratchCard(cardsRepository.getData().first().cards.valueOrNull?.keys?.first()!!)
      val i2 = awaitItem()
      assertTrue("Card should be scratched", i2.cards.valueOrNull?.values?.first()?.isScratched == true)
    }
  }

  @Test(expected = IllegalArgumentException::class)
  fun `when scratchCard with not existing card, function should throw`() = runTest {
    cardsRepository.scratchCard("123")
  }

  @Test
  fun `when removeCard, state should update`() = runTest {
    cardsRepository.generateCard()
    cardsRepository.getData().test {
      val i1 = awaitItem()
      assertEquals(1, i1.cards.valueOrNull?.size)
      cardsRepository.removeCard(cardsRepository.getData().first().cards.valueOrNull?.keys?.first()!!)
      val i2 = awaitItem()
      assertEquals(0, i2.cards.valueOrNull?.size)
    }
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

  @Test
  fun `given stored cards, when repository init, then state should contain cards`() = runTest {
    val cards = listOf(
      ScratchCard("CARD1", false, true),
      ScratchCard("CARD2", true, true)
    )

    whenever(db.loadCards()).thenReturn(cards)

    cardsRepository = CardsRepositoryImpl(db, apiService, TestScope(dispatcher))

    cardsRepository.loadCards()
    cardsRepository.getData().test {
      val i1 = awaitItem().cards.valueOrNull
      assertNotNull(i1)
      assertEquals(cards.size, i1?.size)
    }
  }
}