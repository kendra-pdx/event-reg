package me.enkode.er.backend

import me.enkode.er.backend.auth.{Key, KeyId}
import me.enkode.er.backend.profile.User

case class InMemoryState(
  keys: Map[KeyId, Key] = Map.empty,
  users: Set[User] = Set.empty,
) {

  lazy val usersByEmail: Map[String, User] = users.map(u => u.profile.email -> u).toMap
}
