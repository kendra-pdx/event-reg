package me.enkode.er.backend.framework.auth

object KeyRepository {
  sealed trait Error extends Throwable
  case class KeyNotFoundError(keyId: KeyId) extends Throwable
}

trait KeyRepository[F[_]] {

  def getKey(keyId: KeyId): F[Key]
}
