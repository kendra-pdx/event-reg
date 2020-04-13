package me.enkode.er.backend.module.profile

trait ProfileRepository[F[_]] {
  def findUserByEmail(email: String): F[Option[User]]
}
