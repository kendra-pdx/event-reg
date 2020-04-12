package me.enkode.er.backend.server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import me.enkode.er.backend.framework.log._
import me.enkode.er.backend.module.profile.ProfileEndpoint

import scala.concurrent.ExecutionContextExecutor

object ServerMain extends App with CORSSupport {
  val logger = new ConsoleLogger(this.getClass.getSimpleName, ConsoleLogger.Level.Debug)
  implicit val traceSpan = TraceSpan("serverMain")

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val basePathMatcher = PathMatcher("events")

  val route = List(ProfileEndpoint)
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
