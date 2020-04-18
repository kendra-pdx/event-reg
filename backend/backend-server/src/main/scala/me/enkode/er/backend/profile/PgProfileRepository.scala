package me.enkode.er.backend.profile

import java.time.Instant
import java.util.UUID

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object PgProfileRepository {
  object Tables {
    class Profile(tag: Tag) extends Table[(UUID, String, String, Array[Byte], Instant)](tag, "profile") {
      def id = column[UUID]("id", O.PrimaryKey)
      def email = column[String]("email", O.Unique)
      def fullName = column[String]("full_name")
      def pwHash = column[Array[Byte]]("pw_hash")
      def pwChanged = column[Instant]("pw_changed")
      override def * = (id, email, fullName, pwHash, pwChanged)
    }

    val profiles = TableQuery[Profile]
  }
}

class PgProfileRepository(db: Database)(implicit ec: ExecutionContext) extends ProfileRepository[Future] {
  import PgProfileRepository._

  override def findUserByEmail(email: String): Future[Option[User]] = {
    val q = Tables.profiles.filter(_.email === email)
    for {
      result <- db.run(q.result)
    } yield {
      result.lastOption.map { case (id, email, fullName, pwHash, pwChanged) =>
        User(Profile(
          ProfileId(id.toString),
          fullName,
          email
        )
        )(Password(pwHash, pwChanged))
      }
    }
  }


  /**
   * creates a user if they don't already exist
   * throws DuplicateUserError if the user exists
   */
  override def insert(user: User): Future[User] = {
    val q = Tables.profiles += ((
      UUID.fromString(user.profile.profileId.asString),
      user.profile.email,
      user.profile.fullName,
      user.password.hash,
      user.password.lastChanged
    ))
    for{
      _ <- db.run(q)
    } yield {
      user
    }
  }

  def init(): Future[Unit] = {
    val op = DBIO.seq(
      Tables.profiles.schema.createIfNotExists
    )
    db.run(op)
  }
}
