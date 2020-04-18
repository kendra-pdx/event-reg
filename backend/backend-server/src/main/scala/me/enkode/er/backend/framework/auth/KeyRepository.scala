package me.enkode.er.backend.framework.auth

object KeyRepository {
  sealed trait Error extends Throwable
  case class KeyNotFoundError(keyId: KeyId) extends Throwable
  object CurrentKeyNotFoundError extends Throwable
}

trait KeyRepository[F[_]] {
  import KeyRepository._

  /**
   * find a key by id.
   * throws KeyNotFoundError when the key isn't found
   */
  def getKey(keyId: KeyId): F[Key]

  /**
   * find the most recent, active, unexpired key
   * throws CurrentKeyNotFoundError when there's no current key. you should create a new one.
   */
  def currentKey(): F[Key]

  /**
   * save the specified kye
   */
  def saveKey(key: Key): F[Key]
}
