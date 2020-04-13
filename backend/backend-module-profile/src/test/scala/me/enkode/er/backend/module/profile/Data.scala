package me.enkode.er.backend.module.profile

import java.time.Instant
import java.util.UUID

trait Data {
  val profileA = Profile(ProfileId(UUID.randomUUID().toString), "a", "profile@a.com")
  val userA = User(profileA)(Password(Array.emptyByteArray, Instant.now))
}
