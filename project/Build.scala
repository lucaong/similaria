import sbt._
import Keys._

object Nobootcp {
  import java.io.File._

  def runNobootcpInputTask(configuration: Configuration) = inputTask {
    (argTask: TaskKey[Seq[String]]) => (argTask, streams, fullClasspath in configuration) map { (at, st, cp) =>
      val runCp = cp.map(_.data).mkString(pathSeparator)
      val runOpts = Seq("-classpath", runCp) ++ at
      val result = Fork.java.fork(None, runOpts, None, Map(), true, StdoutOutput).exitValue()
      if (result != 0) sys.error("Run failed")
    }
  }

  val testNobootcp = TaskKey[Unit]("test-nobootcp", "Runs tests without Scala library on the boot classpath")

  val testNobootcpSettings = testNobootcp <<= (streams, productDirectories in Test, fullClasspath in Test) map { (st, pd, cp) =>
    val testCp = cp.map(_.data).mkString(pathSeparator)
    val testExec = "org.scalatest.tools.Runner"
    val testPath = pd(0).toString
    val testOpts = Seq("-classpath", testCp, testExec, "-R", testPath, "-o")
    val result = Fork.java.fork(None, testOpts, None, Map(), false, LoggedOutput(st.log)).exitValue()
    if (result != 0) sys.error("Tests failed")
  }

  val runNobootcp = InputKey[Unit]("run-nobootcp", "Runs main classes without Scala library on the boot classpath")

  val mainRunNobootcpSettings = runNobootcp <<= runNobootcpInputTask(Runtime)
  val testRunNobootcpSettings = runNobootcp <<= runNobootcpInputTask(Test)

  lazy val settings =
    mainRunNobootcpSettings ++
    testRunNobootcpSettings ++
    testNobootcpSettings
}
