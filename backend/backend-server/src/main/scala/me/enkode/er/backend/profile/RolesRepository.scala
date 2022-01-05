package me.enkode.er.backend.profile

object RolesRepository {
  sealed trait RolesRepositoryFailure
  case class RolesRepositoryException(cause: Throwable)
    extends RuntimeException(cause) with RolesRepositoryFailure
}

trait RolesRepository[F[_]] {
  def getProfileRoles(profileId: ProfileId): F[List[Role]]

  def addProfileRole(profileId: ProfileId, role: Role): F[Unit]

  def removeProfileRole(profileId: ProfileId, role: Role): F[Unit]
}
