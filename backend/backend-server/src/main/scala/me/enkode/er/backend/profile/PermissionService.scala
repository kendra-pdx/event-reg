package me.enkode.er.backend.profile

import cats._
import cats.implicits._

object PermissionService {
  import Permission._
  import Role._

  val rolePermissions: Map[Role, Set[Permission]] = Map(
    Anonymous -> Set(LoginProfile, CreateProfile),
    Customer -> Set(LoginProfile, GetMyProfile),
    Organizer -> Set(LoginProfile, GetMyProfile),
    Admin -> Set(LoginProfile, GetMyProfile, GetOtherProfile, CreateProfile, LoginProfile, AssignRole)
  )

  sealed trait PermissionServiceError
  case class PermissionsRepositoryFailure(throwable: Throwable) extends PermissionServiceError
}

class PermissionService[F[_]: MonadError[*[_], Throwable]](rolesRepository: RolesRepository[F]) {
  import PermissionService._

  type Result[T] = F[Either[PermissionServiceError, T]]

  def permissionsOfProfile(profileId: ProfileId): Result[Set[Permission]] = {
    for {
      roles <- rolesRepository.getProfileRoles(profileId)
    } yield {
      roles.toSet
        .flatMap((r: Role) => rolePermissions.getOrElse(r, Set.empty))
        .asRight[PermissionServiceError]
    }
  }

  def assignProfileRoles(profileId: ProfileId, roles: Set[Role]): Result[Unit] = {
    def doWithRoles(f: Role => F[Unit])(roles: Set[Role]): F[Unit] = {
      roles.toList.map(f).sequence.map({_: List[Unit] => ()})
    }

    val removeAll: Set[Role] => F[Unit] =
      doWithRoles(rolesRepository.removeProfileRole(profileId, _))

    val addAll: Set[Role] => F[Unit] =
      doWithRoles(rolesRepository.addProfileRole(profileId, _))

    (for {
      existingRoles <- rolesRepository.getProfileRoles(profileId)
      removes = existingRoles.toSet.diff(roles)
      adds = roles.diff(existingRoles.toSet)
      _ <- removeAll(removes)
      _ <- addAll(adds)
    } yield {
      ().asRight[PermissionServiceError]
    }).recover({
      case rre: RolesRepository.RolesRepositoryException =>
        PermissionsRepositoryFailure(rre).asLeft[Unit]
    })
  }
}
