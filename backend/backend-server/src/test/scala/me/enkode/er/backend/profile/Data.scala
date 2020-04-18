package me.enkode.er.backend.profile

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

import me.enkode.er.backend.auth.{Key, KeyId, KeyType}

trait Data {
  def now(): Instant = Instant.ofEpochSecond(Instant.now.getEpochSecond)

  case class TestKey(
    keyId: KeyId,
    expires: Instant = now().plus(45, ChronoUnit.DAYS),
    notBefore: Instant = now().minus(45, ChronoUnit.DAYS)
  ) extends Key {
    override val keyType: KeyType = KeyType("test")
    override val data: Array[Byte] = UUID.randomUUID().toString.getBytes()
  }

  val keyIdA: KeyId = KeyId("a")
  val keyA: TestKey = TestKey(keyIdA)

  val keyIdB: KeyId = KeyId("b")
  val keyB: TestKey = TestKey(keyIdB)

  val profileA = Profile(ProfileId(UUID.randomUUID().toString), "a", "profile@a.com")
  val profileB = Profile(ProfileId(UUID.randomUUID().toString), "b", "profile@b.com")
  val userA = User(profileA)(Password(Array.emptyByteArray, Instant.now))
  val userB = User(profileB)(Password(Array.emptyByteArray, Instant.now))
}
