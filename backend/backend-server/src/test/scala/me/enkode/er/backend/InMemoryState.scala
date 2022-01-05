package me.enkode.er.backend

import me.enkode.er.backend.auth.{Key, KeyId}
import me.enkode.er.backend.profile.{Profile, ProfileId, Role, User}

case class InMemoryState(
  keys: Map[KeyId, Key] = Map.empty,
  users: Set[User] = Set.empty,
  profileRoles: Map[ProfileId, Set[Role]] = Map.empty,
) {

  lazy val usersByEmail: Map[String, User] = users.map(u => u.profile.email -> u).toMap
  lazy val profilesById: Map[ProfileId, Profile] = users.map(_.profile).map(p => p.profileId -> p).toMap
}
