val CirceVersion = "0.14.1"
val Redis4CatsVersion = "1.7.1"
val TestContainerVersion = "0.41.4"
val Http4sVersion = "0.23.30"

lazy val root = (project in file("."))
  .settings(
    Test / fork := true,
    scalafmtOnCompile := true,
    scalaVersion := "2.13.15",
    scalacOptions ++= Seq(
      "-encoding",
      "utf-8",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:privates",
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:params",
      // "-Xfatal-warnings",
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "com.github.fd4s" %% "fs2-kafka" % "3.5.1",
      "dev.profunktor" %% "redis4cats-effects" % Redis4CatsVersion,
      "dev.profunktor" %% "redis4cats-streams" % Redis4CatsVersion,
      "dev.profunktor" %% "redis4cats-log4cats" % Redis4CatsVersion,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainerVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-kafka" % TestContainerVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-redis" % TestContainerVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-wiremock" % TestContainerVersion % Test
    )
  )
