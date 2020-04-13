import Boilerplate.Modules

lazy val `backend-test-utils` = project.in(file("backend/backend-test-utils"))
  .settings(
    libraryDependencies ++= Seq(
      Modules.catsCore,
      Modules.scalaTest
    )
  )

lazy val `backend-framework` = project.in(file("backend/backend-framework"))
  .settings(
    libraryDependencies ++= Seq(
      Modules.akkaActors,
      Modules.akkaHttp,
      Modules.μPickle,
      Modules.μJson,
      Modules.enumeratum,
      Modules.catsCore,
      Modules.catsMtlCore,
      Modules.catsEffect,
      Modules.slick,
      Modules.slickHikariCp,
      Modules.pgSqlJdbc,
    )
  ).dependsOn(`backend-test-utils` % Test)

lazy val `backend-module-profile` = project.in(file("backend/backend-module-profile"))
  .dependsOn(`backend-framework`)
  .settings(
    libraryDependencies ++= Seq(
      Modules.akkaHttp,
      Modules.akkaHttpCore,
      Modules.slick,
      Modules.slickHikariCp,
      Modules.pgSqlJdbc,
    )
  )

lazy val `backend-server` = project.in(file("backend/backend-server"))
  .dependsOn(
    `backend-framework`,
    `backend-module-profile`,
  )
  .settings(
    libraryDependencies ++= Seq(
      Modules.akkaStreams,
      Modules.akkaHttp,
      Modules.akkaHttpCore,
      Modules.akkaActors,
    ),
    assemblyJarName in assembly := "events-api.jar"
  )

lazy val `event-reg` = project.in(file("."))
  .aggregate(
    `backend-server`,
    `backend-framework`,
    `backend-module-profile`
  )
  .dependsOn(
    `backend-server`,
    `backend-framework`,
    `backend-module-profile`
  )