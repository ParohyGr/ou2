package com.parohy.outwo.scratch.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiServiceTest {
  private lateinit var apiService: ApiService
  private lateinit var mockWebServer: MockWebServer

  @Before
  fun setup() {
    // 1. Initialize MockWebServer
    mockWebServer = MockWebServer()
    mockWebServer.start()

    // 2. Configure Retrofit to use the MockWebServer URL and Gson converter
    apiService = Retrofit.Builder()
      .baseUrl(mockWebServer.url("/"))
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(ApiService::class.java)
  }

  @After
  fun tearDown() {
    // Shut down the server after each test
    mockWebServer.shutdown()
  }

  @Test
  fun `given correct structure, when activate, then return AndroidVersion`() = runTest {
    val expected = "413506"
    val mockJson = """{"ios":"6.42", "iosTM":"1.24", "iosRA":"1.3600", "iosRA_2":"1.3600", "android":$expected, "androidTM":"271780", "androidRA":"471756"}""".trimIndent()

    val response = MockResponse()
      .setResponseCode(200)
      .setBody(mockJson)

    mockWebServer.enqueue(response)

    val result: AndroidVersion = apiService.activateVersion("1234")

    assertNotNull(result)
    assertEquals(expected, result.version)

    val recordedRequest = mockWebServer.takeRequest()
    assertEquals("/version?code=1234", recordedRequest.path)
  }

  @Test(expected = HttpException::class)
  fun `given incorrect structure, when activate, then throws`() = runTest {
    val response = MockResponse()
      .setResponseCode(500)
      .setBody("Server blew up")

    mockWebServer.enqueue(response)

    apiService.activateVersion("1234")
  }
}