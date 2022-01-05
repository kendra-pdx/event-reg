package me.enkode.er.backend.profile

import java.time.Instant

import me.enkode.er.backend.auth.AuthToken

case class ProfileId(asString: String) extends AnyVal
case class Password(hash: Array[Byte], lastChanged: Instant, schemeVersion: Int = 0)

case class Profile(profileId: ProfileId, fullName: String, email: String)
case class User(profile: Profile)(val password: Password)
case class Login(user: User, authToken: AuthToken, refreshToken: AuthToken)

case class PermissionId(asString: String) extends AnyVal
sealed abstract class Permission(val permissionId: PermissionId)
object Permission {
  case object RefreshToken extends Permission(PermissionId("token.refresh"))

  case object GetMyProfile extends Permission(PermissionId("profile.my.get"))
  case object GetOtherProfile extends Permission(PermissionId("profile.other.get"))
  case object CreateProfile extends Permission(PermissionId("profile.create"))
  case object LoginProfile extends Permission(PermissionId("profile.login"))
  case object AssignRole extends Permission(PermissionId("profile.role.assign"))
}

case class RoleId(asString: String) extends AnyVal
sealed abstract class Role(val roleId: RoleId)
object Role {
  case object Admin extends Role(RoleId("admin"))
  case object Anonymous extends Role(RoleId("anonymous"))
  case object Customer extends Role(RoleId("customer"))
  case object Organizer extends Role(RoleId("organizer"))

}