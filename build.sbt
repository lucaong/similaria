import AssemblyKeys._

assemblySettings

name := "similaria"

organization := "com.lucaongaro"

version := "0.0.1"

scalaVersion := "2.10.2"

scalacOptions += "-feature"

resolvers += "Fusesource Repository" at "http://repo.fusesource.com/nexus/content/groups/public-snapshots"

libraryDependencies += "org.fusesource.lmdbjni" % "lmdbjni-all" % "99-master-SNAPSHOT"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "commons-io" % "commons-io" % "2.3" % "test"

libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.7"

fork := true
