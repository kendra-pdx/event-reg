package me.enkode.er.backend.profile

import cats.data.StateT
import cats.implicits._
import cats.mtl.implicits._
import me.enkode.er.backend.InMemoryState
import org.scalatest.EitherValues
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers

class PermissionServiceTest extends AnyFeatureSpec with Matchers with EitherValues with Data {
  type TestState[A] = StateT[Either[Throwable, *], InMemoryState, A]

  val repository = new InMemoryRolesRepository[TestState[*]]()
  val service = new PermissionService[TestState[*]](repository)

  val emptyState: InMemoryState = InMemoryState()
  val basicState: InMemoryState = InMemoryState(
    profileRoles = Map(
      profileA.profileId -> Set(Role.Admin),
    )
  )

  Feature("getting the permissions for a profile") {
    Scenario("roles for a profile that has no mappings should be empty") {
      val permissions = service.permissionsOfProfile(profileB.profileId)
          .runA(emptyState).valueOr(throw _)

      permissions mustBe Right(Set.empty)
    }

    Scenario("roles for a profile that exist should be permissions for all roles") {
      val permissions = service.permissionsOfProfile(profileA.profileId)
        .runA(basicState).valueOr(throw _).getOrElse(fail("must be right"))

      permissions mustBe PermissionService.rolePermissions(Role.Admin)
    }
  }

  Feature("assigning roles to a profile") {
    Scenario("assigning roles to an empty profile should yield a profile with those roles") {
      val state = service.assignProfileRoles(profileA.profileId, Set(Role.Customer))
        .runS(emptyState).valueOr(throw _)

      state.profileRoles(profileA.profileId) mustBe Set(Role.Customer)
    }

    Scenario("assigning roles to a profile with should yield a profile with those roles") {
      val state = service.assignProfileRoles(profileA.profileId, Set(Role.Customer, Role.Anonymous))
        .runS(basicState).valueOr(throw _)

      state.profileRoles(profileA.profileId) mustBe Set(Role.Customer, Role.Anonymous)
    }
  }

}
