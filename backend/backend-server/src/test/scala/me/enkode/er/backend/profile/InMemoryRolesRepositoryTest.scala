package me.enkode.er.backend.profile

import cats.implicits._
import cats.mtl.implicits._
import cats.data.StateT
import me.enkode.er.backend.InMemoryState
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class InMemoryRolesRepositoryTest extends AnyFunSuite with Matchers with Data {
  type TestState[A] = StateT[Either[Throwable, *], InMemoryState, A]

  val repository = new InMemoryRolesRepository[TestState[*]]()
  val emptyState: InMemoryState = InMemoryState()
  val basicState: InMemoryState = InMemoryState(
    profileRoles = Map(
      profileA.profileId -> Set(Role.Admin),
    )
  )

  test("getting from an empty state yields and empty list") {
    val roles = repository.getProfileRoles(profileA.profileId)
      .runA(emptyState).valueOr(throw _)

    roles.size mustBe 0
  }

  test("getting from an exiting profile gets their roles") {
    val roles = repository.getProfileRoles(profileA.profileId)
      .runA(basicState).valueOr(throw _)
    roles.toSet mustBe Set(Role.Admin)
  }

  test("adding a role to an existing profile adds the role") {
    val state = repository.addProfileRole(profileA.profileId, Role.Customer)
      .runS(basicState).valueOr(throw _)

    state.profileRoles.getOrElse(profileA.profileId, Set.empty) must contain (Role.Customer)
  }

  test("removing a role from an existing profile removes the role") {
    val state = repository.removeProfileRole(profileA.profileId, Role.Admin)
      .runS(basicState).valueOr(throw _)

    state.profileRoles.getOrElse(profileA.profileId, Set.empty) must not contain Role.Admin
  }

  test("adding a role to a new profile adds the role") {
    val state = repository.addProfileRole(profileB.profileId, Role.Customer)
      .runS(basicState).valueOr(throw _)

    state.profileRoles.size must be(2)
    state.profileRoles.getOrElse(profileB.profileId, Set.empty) must contain (Role.Customer)
  }

  test("removing a role from a new profile is a no-op") {
    val state = repository.removeProfileRole(profileB.profileId, Role.Admin)
      .runS(basicState).valueOr(throw _)

    state.profileRoles.getOrElse(profileB.profileId, Set.empty) must be (Set.empty)
  }

}
