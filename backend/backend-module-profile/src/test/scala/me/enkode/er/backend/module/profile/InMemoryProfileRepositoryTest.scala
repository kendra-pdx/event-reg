package me.enkode.er.backend.module.profile

import cats.data.StateT
import cats.implicits._
import cats.mtl.implicits._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class InMemoryProfileRepositoryTest extends AnyFunSuite with Matchers with Data {
  type TestState[A] = StateT[Either[Throwable, *], InMemoryState, A]

  val repository = new InMemoryProfileRepository[TestState[*]]()

  test("when a user doesn't exist, return none") {
    val user = repository.findUserByEmail("none@enkode.me")
      .runA(InMemoryState()).valueOr(throw _)

    user must be(None)
  }

  test("when a user exists, return it") {
    val user = repository.findUserByEmail(userA.profile.email)
      .runA(InMemoryState(users = List(userA))).valueOr(throw _)

    user must be(Some(userA))
  }
}
