import Boilerplate.Modules

lazy val commonSettings = Seq(
  logLevel in assembly := Level.Info
)

lazy val `backend-test-utils` = project.in(file("backend/backend-test-utils"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Modules.catsCore,
      Modules.scalaTest
    )
  )

lazy val `backend-server` = project.in(file("backend/backend-server"))
  .dependsOn(`backend-test-utils` % Test)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Modules.akkaActors,
      Modules.akkaHttp,
      Modules.akkaHttpCore,
      Modules.akkaStreams,
      Modules.catsCore,
      Modules.catsEffect,
      Modules.catsMtlCore,
      Modules.enumeratum,
      Modules.monocleCore,
      Modules.monocleMacro,
      Modules.pgSqlJdbc,
      Modules.slick,
      Modules.slickHikariCp,
      Modules.μJson,
      Modules.μPickle,
    ),
    assemblyJarName in assembly := "events-api.jar"
  )

lazy val `event-reg` = project.in(file("."))
  .settings(commonSettings)
  .aggregate(
    `backend-server`,
  )
  .dependsOn(
    `backend-server`,
  )