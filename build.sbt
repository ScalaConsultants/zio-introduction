import Build._

lazy val root =
  (project in file("."))
    .settings(
      organization := "io.scalac",
      organizationName := "Scalac",
      name := s"zio-introduction",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "3.0.0",
      scalacOptions ++= Seq(
        "-language:postfixOps",
        "-Ykind-projector",
        "-Yexplicit-nulls",
        "-source",
        "3.0",
        "-language:implicitConversions"
      ),
      libraryDependencies ++= Seq(
        "dev.zio" %% "zio" % v.zio
      )
    )
