package me.enkode.er.backend.framework.auth

import cats.data._
import cats.implicits._
import cats.mtl.implicits._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class InMemoryKeyRepositoryTest extends AnyFunSuite with Matchers with Data {
  private val respository = new InMemoryKeyRepository[StateT[Either[Throwable, *], InMemoryState, *]]
//  import Data._

  test("when queried with an id that exists, it should return the key") {
    val initialState = InMemoryState(
      keys = Map(keyIdA -> keyA)
    )

    val key = respository.getKey(keyIdA).runA(initialState).valueOr(throw _)
    key must be(keyA)
  }

  test("when queried with an id that doesn't exist, it should yield an error") {
    val initialState = InMemoryState()
    val key = respository.getKey(keyIdA).runA(initialState)
    key must be(Left(KeyRepository.KeyNotFoundError(keyIdA)))
  }
}
