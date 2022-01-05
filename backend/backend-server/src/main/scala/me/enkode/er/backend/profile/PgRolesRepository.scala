package me.enkode.er.backend.profile

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

object PgRolesRepository {
  object Tables {
    class Roles(tag: Tag) extends Table[(UUID, String)](tag, "roles") {
      def profileId = column[UUID]("profile_id")
      def roleId = column[String]("role_id")

      def pk =
        primaryKey("roles_pk", (profileId, roleId))

      def profileIx =
        index("profile_ix", profileId)

      def profileIdFk =
        foreignKey("profile_fk", profileId, PgProfileRepository.Tables.profiles)(_.id, onDelete = ForeignKeyAction.Cascade)

      override def * = (profileId, roleId)
    }

    val roles = TableQuery[Roles]
  }
}

class PgRolesRepository(db: Database)(implicit ec: ExecutionContext) extends RolesRepository[Future] {
  import PgRolesRepository._
  override def getProfileRoles(profileId: ProfileId): Future[List[Role]] = ???

  override def addProfileRole(profileId: ProfileId, role: Role): Future[Unit] = ???

  override def removeProfileRole(profileId: ProfileId, role: Role): Future[Unit] = ???

  def init(): Future[Unit] = {
    val op = DBIO.seq(
      Tables.roles.schema.createIfNotExists
    )
    db.run(op)
  }
}
