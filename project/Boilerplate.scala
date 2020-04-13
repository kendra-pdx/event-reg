import sbt._, Keys._

object Boilerplate extends AutoPlugin {
  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.13.1",
    organization := "enkode.me",
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint"),
//    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Xfatal-warnings"),
    addCompilerPlugin(Modules.kindProjector),
    libraryDependencies += Modules.scalaTest % Test
  )

  object Modules {
    object Versions {
      val kindProjector = "0.11.0"

      val akkaHttp = "10.1.11"
      val akka = "2.6.4"

      val μPickle = "0.9.5"
      val μJson = "0.9.5"

      val enumeratum = "1.5.15"

      val catsCore = "2.2.0-M1"
      val catsMtlCore = "0.7.0"
      val catsEffect = "2.1.2"

      val scalaTest = "3.1.1"

      val pgSqlJdbc = "42.2.12"
      val slick = "3.3.2"
    }

    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full

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
  }
}