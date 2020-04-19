package me.enkode.er.backend.profile

import cats.data.StateT
import cats.implicits._
import cats.mtl.implicits._
import me.enkode.er.backend.InMemoryState
import me.enkode.er.backend.auth.{AuthService, InMemoryKeyRepository}
import org.scalatest.EitherValues
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers

class ProfileServiceTest extends AnyFeatureSpec with Matchers with EitherValues with Data {
  type TestState[A] = StateT[Either[Throwable, *], InMemoryState, A]
  type FrameworkTestState[A] = StateT[Either[Throwable, *], InMemoryState, A]

  val keyRepository = new InMemoryKeyRepository[TestState[*]]
  val authService = new AuthService[TestState[*]](keyRepository)

  val profileRepository = new InMemoryProfileRepository[TestState[*]]()
  val profileService = new ProfileService[TestState[*]](profileRepository, authService)

  val initialState = InMemoryState(
    keys = Map(keyIdA -> keyA),
    users = Set.empty
  )

  Feature("logging in") {
    Scenario("a valid login") {
      val state = initialState.copy(users = Set(userA))
      val result = profileService.login(userA.profile.email, userAPassword)
        .runA(state).valueOr(throw _)

      val login = result.valueOr(d => fail(s"unexpected: $d"))
      login.user must be(userA)
      login.authToken.header.length must be > 0
      login.authToken.body.length must be > 0
      login.authToken.signature.length must be > 0
    }

    Scenario("an invalid login - no such user") {
      val state = initialState.copy(users = Set(userA))
      val result = profileService.login(userB.profile.email, userBPassword)
        .runA(state).valueOr(throw _)

      result must be (Left(ProfileService.InvalidLogin(userB.profile.email)))
    }

    Scenario("an invalid login - bad password") {
      val state = initialState.copy(users = Set(userA))
      val result = profileService.login(userB.profile.email, "garbage")
        .runA(state).valueOr(throw _)
      result must be (Left(ProfileService.InvalidLogin(userB.profile.email)))
    }
  }

  Feature("creating a user") {
    Scenario(("a valid new user")) {
      val (finalState, result) = profileService.createUser(userA.profile.email, userA.profile.fullName, "password")
          .run(initialState).valueOr(throw _)

      val user = result.valueOr(e => fail(s"unexpected: $e"))
      user.profile.fullName must be(userA.profile.fullName)
      user.profile.email must be(userA.profile.email)
      user.password.hash.length must be > 0

      finalState.users.size must be > 0
      finalState.usersByEmail
      finalState.users.map(_.profile.email) must contain (userA.profile.email)
      finalState.users.map(_.profile.fullName) must contain (userA.profile.fullName)
    }

    Scenario(("a duplicate user by email address")) {
      val (finalState, result) = profileService.createUser(userA.profile.email, userA.profile.fullName, "password")
        .run(initialState.copy(users = Set(userA))).valueOr(throw _)

      val error = result.left.value
      error must matchPattern {
        case (ProfileService.DuplicateUser(User(Profile(_, userA.profile.fullName, userA.profile.email)))) =>
      }

      finalState.users.size must be (1)
      finalState.usersByEmail.keys must contain (userA.profile.email)
    }
  }

  Feature("getting a profile by id") {
    Scenario("lookup by valid id") {
      val result = profileService.findProfileById(userA.profile.profileId)
        .runA(initialState.copy(users = Set(userA))).valueOr(throw _)

      result must be (Right(userA.profile))
    }

    Scenario("lookup profile by invalid id") {
      val profileId = userA.profile.profileId
      val result = profileService.findProfileById(profileId)
        .runA(initialState).valueOr(throw _)

      result must be (Left(ProfileService.ProfileNotFound(profileId)))
    }
  }
}
