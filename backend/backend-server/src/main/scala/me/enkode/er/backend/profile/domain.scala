package me.enkode.er.backend.profile

import java.time.Instant

import me.enkode.er.backend.auth.AuthToken

case class ProfileId(asString: String) extends AnyVal
case class Password(hash: Array[Byte], lastChanged: Instant, schemeVersion: Int = 0)
case class Profile(profileId: ProfileId, fullName: String, email: String)
case class User(profile: Profile)(val password: Password)

case class Login(user: User, authToken: AuthToken, refreshToken: AuthToken)
