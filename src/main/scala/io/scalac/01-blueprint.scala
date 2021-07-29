package io.scalac

/**
 * A blueprint is a **value** that describes a computation.
 *
 * The computation requires a value of type R to run, we say ~ it requires some context R to run ~ it runs in the
 * environment R
 *
 * The computation can end with: ~ a success value of type A ~ an error value of type B ~ it can run forever, never
 * returning ~ or in case of fatal defect, it can throw an exception however this is the "escape hatch" mechanism,
 * ideally it should never happen
 */
final case class Blueprint[-R, +E, +A](run: R => Either[E, A]):
  self =>

  /**
   * EXERCISE 1
   *
   * Using the function `f` construct a new blueprint, with the transformed success value.
   */
  def map[B](f: A => B): Blueprint[R, E, B] =
    ???

  /**
   * EXERCISE 2
   *
   * Using the function `f` construct a new blueprint, with the transformed success value.
   */
  def flatMap[R1 <: R, E1 >: E, B](f: A => Blueprint[R1, E1, B]): Blueprint[R1, E1, B] =
    ???

  /**
   * EXERCISE 3
   *
   * Construct a new blueprint, with the error value moved into the success channel.
   */
  def either: Blueprint[R, Nothing, Either[E, A]] =
    ???

  /**
   * EXERCISE 4
   *
   * Using the provided value, construct a new blueprint which does not require any environment.
   */
  def provide(r: R): Blueprint[Any, E, A] =
    ???

  /**
   * EXERCISE 5
   *
   * Given evidence that the error value is a subtype of Throwable, construct a new blueprint that promises to never
   * fail, by invoking the "escape hatch" mechanism (throwing exception) in case the blueprint does produce the error.
   *
   * Note: this does not deal with the problem. This is supposed to be used in cases where we consider the error to be a
   * fatal failure that we cannot recover from. Using this method is the equivalent of saying: ~ assume happy path or
   * give up otherwise
   */
  def orDie(implicit ev: E <:< Throwable): Blueprint[R, Nothing, A] =
    ???

object Blueprint:

  /**
   * EXERCISE 1
   *
   * Given a value, construct a blueprint that will always succeed with that value.
   */
  def succeed[A](a: => A): Blueprint[Any, Nothing, A] =
    ???

  /**
   * EXERCISE 2
   *
   * Given a value, construct a blueprint that will always fail with that value.
   */
  def fail[E](e: => E): Blueprint[Any, E, Nothing] =
    ???

  /**
   * EXERCISE 3
   *
   * Given a potentially throwing computation, construct a blueprint that will suceed with the computed value or capture
   * the exception in the error channel.
   *
   * Hint: use `scala.util.Try`
   */
  def effect[A](sideEffect: => A): Blueprint[Any, Throwable, A] =
    ???

  /**
   * EXERCISE 4
   *
   * Construct a blueprint surfaces the environment into the success channel.
   */
  def environment[R]: Blueprint[R, Nothing, R] =
    ???

  /**
   * EXERCISE 5
   *
   * Using the function `f` construct a new blueprint, with the transformed environment into success value.
   */
  def access[R, A](f: R => A): Blueprint[R, Nothing, A] =
    ???

  /**
   * EXERCISE 6
   *
   * Using the function `f` construct a new blueprint, with the transformed environment into success value.
   */
  def accessM[R, E, A](f: R => Blueprint[R, E, A]): Blueprint[R, E, A] =
    ???

object console:

  /**
   * EXERCISE 1
   *
   * Construct a blueprint that will print given line to the console.
   *
   * Hint: use the `println` function.
   */
  def putStrLn(line: String): Blueprint[Any, Nothing, Unit] = ???

  /**
   * EXERCISE 2
   *
   * Construct a blueprint that will read a line from the console.
   *
   * Hint: use the `scala.io.StdIn#readLine` function.
   */
  val getStrLn: Blueprint[Any, Nothing, String] = ???

object Runtime:

  /**
   * EXERCISE 1
   *
   * Implement a basic runtime, that will evaluate the blueprints `run` function and return its success value or throw
   * an exception if it evaluates to an error.
   *
   * Hint: use `Either#fold` to deal with both outcomes.
   */
  def unsafeRun[A](blueprint: Blueprint[Any, Throwable, A]): A =
    ???

/**
 * EXERCISE 1
 *
 * Test your implementation, by running the following program from the console.
 *
 * > sbt "runMain io.scalac.helloBlueprint"
 */
@main
def helloBlueprint =
  Runtime.unsafeRun(
    for
      _    <- console.putStrLn("What is your name?")
      name <- console.getStrLn
      _    <- console.putStrLn(s"Hello $name!")
    yield ()
  )
