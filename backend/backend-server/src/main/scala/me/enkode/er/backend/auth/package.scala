package me.enkode.er.backend

import java.time.Instant

package object auth {

}

package auth {

  case class KeyId(asString: String) extends AnyVal

  case class KeyType(asString: String) extends AnyVal

  trait Key {
    val keyId: KeyId
    val keyType: KeyType
    val data: Array[Byte]
    val expires: Instant
    val notBefore: Instant
  }
}