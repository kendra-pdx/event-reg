package me.enkode.er.backend.module.profile

import org.scalatest.featurespec.AnyFeatureSpec

class ProfileServiceTest extends AnyFeatureSpec with Data {
//  type TestState[A] = StateT[Either[Throwable, *], InMemoryState, A]
//  type FrameworkTestState[A] = StateT[Either[Throwable, *], InMemoryState, A]
//
//  val keyRepository = new InMemoryKeyRepository[TestState[*]]
//  val authService = new AuthService[TestState[*]](keyRepository)
//
//  val profileRepository = new InMemoryProfileRepository[TestState[*]]()
//  val profileService = new ProfileService[TestState[*]](profileRepository, authService)

  val initialState = InMemoryState(
    keys = Map(keyIdA -> keyA),
    users = Set.empty
  )

  Feature("logging in") {
    Scenario("a valid login") {
//      profileService.login(userA.profile.email, "???")
//        .runA(initialState.copy(
//          users = Set(userA)
//        )).valueOr(throw _)
      pending
    }

    Scenario("an invalid login") {
      pending
    }
  }

  Feature("creating a user") {
    Scenario(("a valid new user")) {
      pending
    }

    Scenario(("a duplicate user by email address")) {
      pending
    }
  }

  Feature("getting a user by id") {
    Scenario("lookup by valid id") {
      pending
    }
  }
}
