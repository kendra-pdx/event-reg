package me.enkode.er.backend.module.profile

import cats._
import cats.implicits._
import cats.mtl.MonadState

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
}
