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
      .runA(InMemoryState(users = Set(userA))).valueOr(throw _)

    user must be(Some(userA))
  }

  test("insert a new unique user") {
    val (state, created) = repository.insert(userB)
      .run(InMemoryState(users = Set(userA))).valueOr(throw _)

    created must be(userB)
    state.users.find(_ == userB) must be(Some(userB))
  }

  test("insert a new unique user with a duplicate id, fails") {
    val invalidUser = userB.copy(profile = userB.profile.copy(profileId = userA.profile.profileId))(userB.password)
    val result = repository.insert(invalidUser)
      .run(InMemoryState(users = Set(userA)))

    result must be(Left(ProfileRepository.DuplicateUserError(invalidUser)))
  }

  test("insert a new unique user with a duplicate email, fails") {
    val invalidUser = userB.copy(profile = userB.profile.copy(email = userA.profile.email))(userB.password)
    val result = repository.insert(invalidUser)
      .run(InMemoryState(users = Set(userA)))

    result must be(Left(ProfileRepository.DuplicateUserError(invalidUser)))
  }
}
