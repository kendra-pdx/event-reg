package me.enkode.er.backend.module.profile

import me.enkode.er.backend.framework.auth._

case class InMemoryState(
  keys: Map[KeyId, Key] = Map.empty,
  users: Set[User] = Set.empty,
) {

  lazy val usersByEmail: Map[String, User] = users.map(u => u.profile.email -> u).toMap
}
