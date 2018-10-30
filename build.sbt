import sbtcrossproject.{ crossProject, CrossType }
import ScalaModulePlugin._

crossScalaVersions in ThisBuild := List("2.12.6", "2.11.12", "2.13.0-M4")

lazy val bintraySettings = Seq(
  publishMavenStyle := true,
  bintrayOrganization := Some("outworkers"),
  bintrayRepository <<= scalaVersion.apply {
    v => if (v.trim.endsWith("SNAPSHOT")) "oss-snapshots" else "oss-releases"
  },
  bintrayReleaseOnPublish in ThisBuild := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => true },
  licenses := Seq("Apache-2.0" -> url("https://github.com/outworkers/outworkers/blob/develop/LICENSE.txt"))
)

lazy val xml = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(scalaModuleSettings)
  .jvmSettings(scalaModuleSettingsJVM)
  .settings(
    name    := "scala-xml",
    version := "1.2.0-SNAPSHOT",

    // Compiler team advised avoiding the -Xfuture option for releases.
    // The output with -Xfuture should be periodically checked, though.
    scalacOptions         ++= Seq(
      "-deprecation:false",
      "-feature",
      "-Xlint:-stars-align,-nullary-unit"
    ),
    scalacOptions in Test  += "-Xxml:coalescing",

    mimaPreviousVersion := {
      if (System.getenv("SCALAJS_VERSION") == "1.0.0-M5") None // No such release yet
      else Some("1.1.1")
    },

    unmanagedSourceDirectories in Compile ++= {
      (unmanagedSourceDirectories in Compile).value.map { dir =>
        val sv = scalaVersion.value
        CrossVersion.partialVersion(sv) match {
          case Some((2, 13)) => file(dir.getPath ++ "-2.13")
          case _             => file(dir.getPath ++ "-2.11-2.12")
        }
      }
    },

    apiMappings ++= Map(
      scalaInstance.value.libraryJar
        -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/")
    ) ++ {
      // http://stackoverflow.com/questions/16934488
      sys.props.get("sun.boot.class.path").flatMap { classPath =>
        classPath.split(java.io.File.pathSeparator).find(_.endsWith(java.io.File.separator + "rt.jar"))
      }.map { jarPath =>
        Map(
          file(jarPath) -> url("http://docs.oracle.com/javase/8/docs/api")
        )
      } getOrElse {
        // If everything fails, jam in the Java 9 base module.
        Map(
          file("/modules/java.base") -> url("http://docs.oracle.com/javase/9/docs/api"),
          file("/modules/java.xml") -> url("http://docs.oracle.com/javase/9/docs/api")
        )
      }
    }
  ).settings(
    BintrayPlugin.bintrayPublishSettings ++ bintraySettings
  ).jvmSettings(
    OsgiKeys.exportPackage := Seq(s"scala.xml.*;version=${version.value}"),
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.12" % Test,
      "com.novocode" % "junit-interface" % "0.11" % Test,
      "org.apache.commons" % "commons-lang3" % "3.5" % Test,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % Test exclude(
        "org.scala-lang.modules",
        s"scala-xml_${scalaBinaryVersion.value}"
      )
    )
  ).settings(
    BintrayPlugin.bintrayPublishSettings ++ bintraySettings
  ).jvmSettings(
    BintrayPlugin.bintrayPublishSettings ++ bintraySettings
  ).jsSettings(
    // Scala.js cannot run forked tests
    fork in Test := false
  ).jsSettings(BintrayPlugin.bintrayPublishSettings ++ bintraySettings)
  .jsConfigure(_.enablePlugins(ScalaJSJUnitPlugin))

lazy val xmlJVM = xml.jvm
lazy val xmlJS = xml.js
