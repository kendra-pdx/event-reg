package me.enkode.er.backend.profile

import cats._
import cats.implicits._
import cats.mtl.MonadState
import me.enkode.er.backend.InMemoryState
import me.enkode.er.backend.profile.ProfileRepository.DuplicateUserError
import me.enkode.er.backend.profile.ProfileRepository

class InMemoryProfileRepository[F[_]: MonadError[*[_], Throwable]](
  implicit S: MonadState[F, InMemoryState]
) extends ProfileRepository[F] {
  override def findUserByEmail(email: String): F[Option[User]] = {
    for {
      user <- S.inspect(_.usersByEmail.get(email))
    } yield {
      user
    }
  }

  /**
   * creates a user if they don't already exist
   * throws DuplicateUserError if the user exists
   */
  override def insert(user: User): F[User] = {
    for {
      state <- S.get
      _ <- state.users.find(_.profile.profileId == user.profile.profileId).fold({}.pure[F])(_ => {
        DuplicateUserError(user).raiseError[F, Unit]
      })
      _ <- state.users.find(_.profile.email == user.profile.email).fold({}.pure[F])(_ => {
        DuplicateUserError(user).raiseError[F, Unit]
      })
      _ <- S.modify(s => s.copy(users = s.users + user))
    } yield {
      user
    }
  }
}
