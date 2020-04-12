package me.enkode.er.backend.module.profile

case class ProfileId(asString: String) extends AnyVal
case class Profile(profileId: ProfileId, fullName: String)
