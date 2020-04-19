package me.enkode.er.backend.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import cats.implicits._
import me.enkode.er.backend.auth.{AuthService, PgKeyRepository}
import me.enkode.er.backend.framework.CORSSupport
import me.enkode.er.backend.framework.log._
import me.enkode.er.backend.profile.{PgProfileRepository, ProfileEndpoint, ProfileService}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent._
import scala.concurrent.duration._

object ServerMain extends App with CORSSupport {
  import me.enkode.er.backend.framework.ErrorResponse.akkaErrorHandler
  import ServerRejectionHandler._

  val logger = new ConsoleLogger(this.getClass.getSimpleName, ConsoleLogger.Level.Debug)
  implicit val traceSpan = TraceSpan("serverMain")

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val basePathMatcher = PathMatcher("events")

  val db = Database.forConfig("db.pgsql")
  val dbEc = ExecutionContext.fromExecutor(null)

  val keyRepository = new PgKeyRepository(db)(dbEc)
  val authService = new AuthService(keyRepository)

  val profileRepository = new PgProfileRepository(db)(dbEc)
  val profileService = new ProfileService(profileRepository, authService)

  val profileEndpoint = new ProfileEndpoint(profileService, authService)

  val init = (for {
    _ <- keyRepository.init()
    _ <- profileRepository.init()
    _ <- authService.generateKey()
  } yield {
    logger.info("DB Init Successful")
  }).recover({
    case t: Throwable =>
      logger.error(s"DB Init Failed: $t")
  })

  Await.result(init, 60.seconds)

  val route = List(profileEndpoint)
    .tapEach(e => logger.info(s"registering endpoint: ${e.name}"))
    .map(_.createRoute(basePathMatcher))
    .reduce(_ ~ _)

  val port = 8080
  private val http = Http()
  val bindingFuture = http.bindAndHandle(withCORS(route), "0.0.0.0", port)

  logger.info(s"listening on $port")

  actorSystem.registerOnTermination({
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => actorSystem.terminate()) // and shutdown when done
  })
}
