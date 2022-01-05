import sbt._, Keys._

object Boilerplate extends AutoPlugin {
  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.13.7",
    organization := "enkode.me",
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint"),
//    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Xfatal-warnings"),
    addCompilerPlugin(Modules.kindProjector),
//    addCompilerPlugin(Modules.paradiseMacros),
    libraryDependencies += Modules.scalaTest % Test
  )

  object Modules {
    object Versions {
      val kindProjector = "0.13.2"

      val akkaHttp = "10.2.6"
      val akka = "2.6.18"

      val μPickle = "1.4.3"
      val μJson = "1.4.3"

      val enumeratum = "1.7.0"

      val catsCore = "2.7.0"
      val catsMtlCore = "0.7.1"
      val catsEffect = "3.3.0"

      val scalaTest = "3.2.9"

      val pgSqlJdbc = "42.3.1"
      val slick = "3.3.3"

      val monocle = "2.1.0"
    }

    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full
    lazy val paradiseMacros = "org.scalamacros" %% "paradise" % "2.1.1" cross CrossVersion.full

    lazy val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % Versions.akkaHttp
    lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp
    lazy val akkaStreams =  "com.typesafe.akka" %% "akka-stream" % Versions.akka
    lazy val akkaActors = "com.typesafe.akka" %% "akka-actor" % Versions.akka

    lazy val μPickle = "com.lihaoyi" %% "upickle" % Versions.μPickle
    lazy val μJson = "com.lihaoyi" %% "ujson" % Versions.μJson

    lazy val enumeratum = "com.beachape" %% "enumeratum" % Versions.enumeratum

    lazy val catsCore = "org.typelevel" %% "cats-core" % Versions.catsCore
    lazy val catsMtlCore = "org.typelevel" %% "cats-mtl-core" % Versions.catsMtlCore
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

    lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest

    lazy val pgSqlJdbc =  "org.postgresql" % "postgresql" % Versions.pgSqlJdbc
    lazy val slick = "com.typesafe.slick" %% "slick" % Versions.slick
    lazy val slickHikariCp = "com.typesafe.slick" %% "slick-hikaricp" % Versions.slick

    lazy val monocleCore = "com.github.julien-truffaut" %%  "monocle-core" % Versions.monocle
    lazy val monocleMacro = "com.github.julien-truffaut" %%  "monocle-macro" % Versions.monocle
  }
}