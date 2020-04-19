package me.enkode.er.backend.profile

import java.time.Instant
import java.util.UUID

import me.enkode.er.backend.auth.{Data => AuthData}

trait Data extends AuthData {
  val profileA = Profile(ProfileId(UUID.randomUUID().toString), "a", "profile@a.com")
  val profileB = Profile(ProfileId(UUID.randomUUID().toString), "b", "profile@b.com")

  val userAPassword = "userAPassword"
  val userBPassword = "userAPassword"

  val userA = User(profileA)(Password(ProfileService.hash(userAPassword, profileA.email), Instant.now))
  val userB = User(profileB)(Password(ProfileService.hash(userBPassword, profileB.email), Instant.now))
}
