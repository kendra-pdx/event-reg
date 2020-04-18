package me.enkode.er.backend.framework.auth

import cats._
import cats.implicits._
import cats.mtl.MonadState

class InMemoryKeyRepository[F[_]: MonadError[*[_], Throwable]](
  implicit S: MonadState[F, InMemoryState])
  extends KeyRepository[F] {

  override def getKey(keyId: KeyId): F[Key] = {
    for {
      s <- S.inspect(_.keys.get(keyId))
      k <- s.fold(KeyRepository.KeyNotFoundError(keyId).raiseError[F, Key])(_.pure[F])
    } yield {
      k
    }
  }

  override def currentKey(): F[Key] = {
    for {
      kid <- S.inspect(_.keys.keys.last)
      key <- getKey(kid)
    } yield {
      key
    }
  }

  /**
   * save the specified kye
   */
  override def saveKey(key: Key): F[Key] = for {
    _ <- S.modify(s => s.copy(keys = s.keys.updated(key.keyId, key)))
  } yield {
    key
  }
}
