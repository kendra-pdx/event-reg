package me.enkode.er.backend.module.profile

object ProfileRepository {
  case class DuplicateUserError(user: User) extends RuntimeException("duplicate user")
}

trait ProfileRepository[F[_]] {
  /**
   * finds a user by email address, None if not found
   */
  def findUserByEmail(email: String): F[Option[User]]

  /**
   * creates a user if they don't already exist
   * throws DuplicateUserError if the user exists
   */
  def insert(user: User): F[User]
}
