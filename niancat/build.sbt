// import Dependencies._

val Http4sVersion = "0.15.11a"

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "io.github.dandeliondeathray",
      scalaVersion := "2.12.10",
      version := "1.0rc1"
    )
  ),
  name := "niancat",
  exportJars := true,
  retrieveManaged := true,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.1",
  libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test",
  libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe" % Http4sVersion,
    "org.http4s" %% "http4s-dsl" % Http4sVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.1",
    "io.circe" %% "circe-generic" % "0.6.1"
  )
)
