package me.enkode.er.backend.framework.auth

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

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

  val validAuthInfo: AuthInfo = {
    import AuthInfo._
    AuthInfo(
      IssuerEncoding("todo", keyIdA),
      Subject("todo"),
      Audience("todo"),
      Expires(now.plus(30, ChronoUnit.DAYS)),
      NotBefore(now.minus(30, ChronoUnit.DAYS)),
      IssuedAt(now),
      List(Scope("read")),
    )
  }
}
