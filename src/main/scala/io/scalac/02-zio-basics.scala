package io.scalac.zio_basics

import zio._

/**
 * The following application is the ZIO-based equivalent to the blueprint example.
 *
 * > sbt "runMain io.scalac.zio_basics.helloZIO"
 */
@main
def helloZIO =
  Runtime.default.unsafeRun(
    for
      _    <- Console.printLine("What is your name?")
      name <- Console.readLine
      _    <- Console.printLine(s"Hello $name!")
    yield ()
  )

object ErrorFallback extends App:

  val failing  = ZIO.fail("Oopsie!")
  val fallback = ZIO.succeed("Yaay!")

  /**
   * EXERCISE
   *
   * Use ZIO#orElse to transform a failing program into one that cannot fail.
   */
  val program = failing

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.exitCode

object ErrorFold extends App:

  val failing = ZIO.fail("Oopsie!")

  /**
   * EXERCISE
   *
   * Use ZIO#fold to transform a failing program into one that cannot fail.
   */
  val program = failing

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.exitCode

object ErrorFoldM extends App:

  val failing = ZIO.fail("Oopsie!")

  /**
   * EXERCISE
   *
   * Use ZIO#foldM to transform print the success or failure using Console#putStrLn.
   */
  val program = failing

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.exitCode

object Ignore extends App:

  val failing = ZIO.fail("Oopsie!")

  /**
   * EXERCISE
   *
   * Use ZIO#ignore to discard the failure. Note the resulting type signature and share your findings.
   */
  val program = failing

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.exitCode

object Recurse extends App:

  /**
   * > sbt "intro/runMain io.scalac.Recurse"
   */
  val getAge: ZIO[Has[Console], Any, Int] =
    val retry: ZIO[Has[Console], Any, Int] =
      Console.printLine("Please enter a number. Try again.") *> getAge

    Console.readLine
      .flatMap(input => ZIO.attempt(input.toInt))
      .orElse(retry)

  val program =
    for
      _   <- Console.printLine("What is your age?")
      age <- getAge
      _   <- Console.printLine(s"You entered $age.")
    yield age

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    program.exitCode
