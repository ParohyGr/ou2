package com.parohy.outwo.scratch

import com.parohy.outwo.scratch.core.*
import org.junit.Assert.*
import org.junit.Test

class CoreStateTest {
  @Test
  fun `given state is Loading, get isLoading should return true`() {
    val state: State<Throwable, String> = Loading
    assertTrue("isLoading should return true", state.isLoading)
  }

  @Test
  fun `given state is not Loading, get isLoading should return false`() {
    val state: State<Throwable, String> = Failure(Throwable())
    assertFalse("isLoading should return false", state.isLoading)
  }

  @Test
  fun `given state is Content, get isContent should return true`() {
    val state: State<Throwable, String> = Content("Success")
    assertTrue("isContent should return true", state.isContent)
  }

  @Test
  fun `given state is Content, get valueOrNull should return value`() {
    val value = "Success"
    val state: State<Throwable, String> = Content(value)
    assertNotNull("valueOrNull should not be null", state.valueOrNull)
    assertEquals(value, state.valueOrNull)
  }

  @Test
  fun `given state is not Content, get isContent should return false`() {
    val state: State<Throwable, String> = Loading
    assertFalse("isContent should return false", state.isContent)
  }

  @Test
  fun `given state is Failure, get isFailure should return true`() {
    val state: State<Throwable, String> = Failure(Throwable())
    assertTrue("isFailure should return true", state.isFailure)
  }

  @Test
  fun `given state is not Failure, get isFailure should return false`() {
    val state: State<Throwable, String> = Loading
    assertFalse("isFailure should return false", state.isFailure)
  }

  @Test
  fun `given state is Failure, get failureOrNull should return failure`() {
    val value = "Error"
    val state: State<Throwable, String> = Failure(Throwable(value)) 
    assertNotNull("failureOrNull should not be null", state.failureOrNull)
    assertEquals(value, state.failureOrNull?.message)
  }

  @Test
  fun `given result is Result isSuccess, toState should return Content`() {
    val value = "Success"
    val result = Result.success(value)
    val state: State<Throwable, String> = result.toState()
    assertTrue("toState should return Content", state.isContent)
    assertEquals(value, state.valueOrNull)
  }

  @Test
  fun `given result is Result isFailure, toState should return Content`() {
    val value = "Error"
    val result = Result.failure<String>(Throwable(value))
    val state: State<Throwable, String> = result.toState()
    assertTrue("toState should return Failure", state.isFailure)
    assertEquals(value, state.failureOrNull?.message)
  }
}