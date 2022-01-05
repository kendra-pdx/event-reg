package me.enkode.er.backend.profile

import cats._
import cats.implicits._
import cats.mtl.MonadState
import me.enkode.er.backend.InMemoryState

class InMemoryRolesRepository[F[_] : MonadError[*[_], Throwable]](
  implicit S: MonadState[F, InMemoryState]
) extends RolesRepository[F] {
  override def getProfileRoles(profileId: ProfileId): F[List[Role]] = {
    for {
      roles <- S.inspect(_.profileRoles.getOrElse(profileId, Set.empty))
    } yield {
      roles.toList
    }
  }

  override def addProfileRole(profileId: ProfileId, role: Role): F[Unit] = {
    def removeRole(state: InMemoryState): InMemoryState = {
      state.copy(
        profileRoles = state.profileRoles.updated(
          profileId,
          state.profileRoles.getOrElse(profileId, Set.empty) + role
        )
      )
    }

    for {
      _ <- S.modify(removeRole)
    } yield {
      ()
    }
  }

  override def removeProfileRole(profileId: ProfileId, role: Role): F[Unit] = {
    def addRole(state: InMemoryState): InMemoryState = {
      state.copy(
        profileRoles = state.profileRoles.updated(
          profileId,
          state.profileRoles.getOrElse(profileId, Set.empty) - role
        )
      )
    }

    for {
      _ <- S.modify(addRole)
    } yield {
      ()
    }

  }
}
