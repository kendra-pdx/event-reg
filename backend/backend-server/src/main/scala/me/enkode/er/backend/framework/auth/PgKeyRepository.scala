package me.enkode.er.backend.framework.auth

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._

object PgKeyRepository {
  object Tables {
    class Key(tag: Tag) extends Table[(String, String, Array[Byte], Instant, Instant)](tag, "keys") {
      def id = column[String]("id", O.PrimaryKey)
      def keyType = column[String]("key_type", O.Default("shared"))
      def data = column[Array[Byte]]("data")
      def expires = column[Instant]("expires")
      def notBefore = column[Instant]("not_before")

      override def * = (id, keyType, data, expires, notBefore)
    }
    val keys = TableQuery[Key]
  }
}
class PgKeyRepository(db: Database)(implicit ec: ExecutionContext) extends KeyRepository[Future] {
  import PgKeyRepository._
  import KeyRepository._

  override def getKey(keyId: KeyId): Future[Key] = {
    val q = Tables.keys.filter(_.id === keyId.asString)
    for {
      result <- db.run(q.result)
    } yield {
      (result.headOption.map { case (_id, _keyType, _data, _expires, _notBefore) =>
        new Key {
          override val keyId: KeyId = KeyId(_id)
          override val keyType: KeyType = KeyType(_keyType)
          override val data: Array[Byte] = _data
          override val expires: Instant = _expires
          override val notBefore: Instant = _notBefore
        }
      }).getOrElse(throw KeyNotFoundError(keyId))
    }
  }

  override def currentKey(): Future[Key] = {
    val now = Instant.now()
    val q = for {
      k <- Tables.keys
        if k.notBefore <= now
        if k.expires >= now
    } yield k

    for {
      result <- db.run(q.sortBy(_.expires.desc).result)
    } yield {
      (result.headOption.map { case (_id, _keyType, _data, _expires, _notBefore) =>
        new Key {
          override val keyId: KeyId = KeyId(_id)
          override val keyType: KeyType = KeyType(_keyType)
          override val data: Array[Byte] = _data
          override val expires: Instant = _expires
          override val notBefore: Instant = _notBefore
        }
      }).getOrElse(throw CurrentKeyNotFoundError)
    }
  }

  override def saveKey(key: Key): Future[Key] = {
    val q = DBIO.seq(
      Tables.keys += ((key.keyId.asString, key.keyType.asString, key.data, key.expires, key.notBefore))
    )

    for {
      _ <- db.run(q)
    } yield {
      key
    }
  }

  def init(): Future[Unit] = {
    val op = DBIO.seq(
      Tables.keys.schema.createIfNotExists
    )
    db.run(op)
  }
}
