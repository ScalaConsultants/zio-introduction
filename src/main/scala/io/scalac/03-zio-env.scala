package io.scalac.zio_env

import scala.collection.{mutable => m}
import zio._

/**
 * ZIO environment is a modern, type-safe and composable take on dependency injection.
 *
 * Even the smallest microservices must handle many diffrent concerns such as caching, logging,
 * tracing, persistance, business logic or API listening to messages from the outside world.
 *
 * Lets assume we have a simple PetStore model:
 */
opaque type TagId <: Int = Int
object TagId:
  def fromInt(id: Int): TagId = id

opaque type TagName <: String = String
object TagName:
  def fromString(name: String): TagName = name

case class Tag(id: TagId, name: TagName)
object Tag:
  def fromInt(n: Int): Tag = Tag(TagId.fromInt(n), TagName.fromString(s"Tag #$n"))

opaque type PetId <: Int = Int
object PetId:
  def fromInt(id: Int): PetId = id

opaque type PetName <: String = String
object PetName:
  def fromString(name: String): PetName = name

enum PetStatus:
  case Available, Pending, Sold

case class Pet(id: PetId, name: PetName, tags: List[Tag], status: PetStatus)

/**
 * Programming to an interface is a well estabilished best practice.
 * Lets break up our application concerns into well defined interfaces.
 */
trait Logger: // operational concern - visibility
  def info(msg: String): UIO[Unit]
  def error(msg: String): UIO[Unit]
  def debug(msg: String): UIO[Unit]

trait Cache[K, V]: // techncial concern - performance
  def get(key: K): UIO[Option[V]]
  def set(key: K, value: V): UIO[Unit]
  def unset(key: K): UIO[Unit]

trait Database[K, V]: // technical concern - persistance
  def find(id: K): UIO[Option[V]]
  def findAll: UIO[List[(K, V)]]
  def insert(value: V): UIO[K]
  def upsert(id: K, value: V): UIO[Unit]
  def delete(id: K): IO[Unit, Unit]

trait TagRepository: // business concern - labels
  def create(name: TagName): IO[Unit, TagId]
  def find(id: TagId): UIO[Option[Tag]]
  def delete(id: TagId): IO[Unit, Unit]

trait PetRepository: // business concern - inventory
  def register(name: PetName, tags: List[Tag]): UIO[PetId]
  def find(id: PetId): UIO[Option[Pet]]
  def findByStatus(status: PetStatus): UIO[List[Pet]]
  def findByTag(id: TagId): UIO[List[Pet]]
  def updateStatus(id: PetId, status: PetStatus): IO[Unit, Unit]
  def addTag(id: PetId, tag: TagId): IO[Unit, Unit]
  def removeTag(id: PetId, tag: TagId): IO[Unit, Unit]

/**
 * The interfaces above define well-defined contracts and can be used to define loosely coupled dependencies.
 * The dependant services don't know and don't care about the implementation details.
 *
 * This gives us the flexibility to provide any instance with diffrent implementation, for example for test purposes.
 */

// simple logger printing to console
class ConsoleLogger extends Logger:

  private def log(level: String)(msg: String): UIO[Unit] = ZIO.succeed(println(s"[$level] $msg"))

  def info(msg: String): UIO[Unit]  = log("info")(msg)
  def error(msg: String): UIO[Unit] = log("error")(msg)
  def debug(msg: String): UIO[Unit] = log("debug")(msg)

// test logger, accumulating messages and providing a way to check them later
class TestLogger(ref: Ref[List[(String, String)]]) extends Logger:

  private def log(level: String)(msg: String): UIO[Unit] = ref.update(_ :+ (level -> msg))

  def info(msg: String): UIO[Unit]  = log("info")(msg)
  def error(msg: String): UIO[Unit] = log("error")(msg)
  def debug(msg: String): UIO[Unit] = log("debug")(msg)

  val messages: UIO[List[(String, String)]] = ref.get

/**
 * Our application will consist of a foundation - basic low-level services, such as Console or Random,
 * which themselves rely only on JVM, and by proxy, underlaying OS instructions.
 *
 * However the vast majority of our services will be built ontop of those. Each of your domains will likely
 * to be internally technically divided into layers. The higher in the service chain, the closer you are to
 * the surface API of your application, with business logic usually living somewhere in the middle, and persistance
 * or networking usually being at the bottom.
 */

class TagInMemoryCache(
  ref: Ref[(Map[TagId, TagName], List[TagId])],
  log: Logger // traditional constructor-based dependency injection
) extends Cache[TagId, TagName]:

  def get(key: TagId): UIO[Option[TagName]] =
    log.debug(s"Getting key #$key") *>
      ref.get.map { case (cache, _) =>
        cache.get(key)
      }.flatMap { res =>
        log.info(if (res.isEmpty) "miss!" else "hit!").as(res)
      }

  // simple eviction policy - keep 5 most recently added items
  def set(key: TagId, value: TagName): UIO[Unit] =
    log.debug(s"Setting key #$key") *>
      ref.getAndUpdate {
        case (cache, key0 :: keys) if keys.length >= 4 => cache.updated(key, value).removed(key0) -> (keys :+ key)
        case (cache, keys)                             => cache.updated(key, value)               -> (keys :+ key)
      }.flatMap {
        case (_, key0 :: keys) if keys.length >= 4 => log.debug(s"Removing key #$key0")
        case _                                     => ZIO.unit
      }

  def unset(key: TagId): UIO[Unit] =
    log.debug(s"Unsetting key #$key") *>
      ref.update { case (cache, ids) => cache.removed(key) -> ids.filterNot(_ == key) }.unit

object TagInMemoryCache:

  def make(logger: Logger): UIO[TagInMemoryCache] =
    val initCache = Map.empty[TagId, TagName]
    val initKeys  = List.empty[TagId]

    Ref
      .make(initCache -> initKeys)
      .map(ref => TagInMemoryCache(ref, logger))

@main
def tagsCacheDemo =
  Runtime.default.unsafeRun(
    for
      logger <- ZIO.succeed(new ConsoleLogger)
      cache  <- TagInMemoryCache.make(logger)
      producer <- Random
                    .nextIntBetween(1, 11)
                    .map(Tag.fromInt)
                    .flatMap(tag => cache.set(tag.id, tag.name))
                    .repeatN(30)
                    .fork
      consumer <- Random
                    .nextIntBetween(1, 11)
                    .map(TagId.fromInt)
                    .flatMap(id => cache.get(id))
                    .repeatN(30)
                    .fork
      _ <- producer.join
      _ <- consumer.join
      _ <- logger.info("Finished")
    yield ()
  )

/**
 * However with constructor-based dependency injection threading dependencies through application
 * from where they are created to where they are used generates a lot of boilerplate.
 */

class InMemoryTagRepository(ref: Ref.Synchronized[(Map[TagId, TagName], Int)], cache: Cache[TagId, TagName]):
  def create(name: TagName): IO[Unit, TagId] =
    ref.modifyZIO { case (db, lastId) =>
      db.values
        .find(_ == name)
        .fold {
          val nextId    = lastId + 1
          val tagId     = TagId.fromInt(nextId)
          val nextState = db.updated(tagId, name) -> nextId
          ZIO.succeed(tagId -> nextState)
        }(_ => ZIO.fail(()))
    }

  def find(id: TagId): UIO[Option[Tag]] =
    cache
      .get(id)
      .flatMap {
        case None =>
          ref.get.flatMap { case (db, _) =>
            db.get(id) match
              case Some(tagName) => cache.set(id, tagName) *> ZIO.some(tagName)
              case None          => ZIO.none
          }
        case hit => ZIO.succeed(hit)
      }
      .map(_.map(Tag(id, _)))

  def delete(id: TagId): IO[Unit, Unit] =
    ref.updateZIO { case (db, lastId) =>
      def attemptRemove: IO[Unit, (Map[TagId, TagName], Int)] =
        if db.contains(id) then ZIO.succeed(db.removed(id) -> lastId)
        else ZIO.fail(())

      cache.unset(id) *> attemptRemove
    }

object InMemoryTagRepository:
  def make(cache: Cache[TagId, TagName]): UIO[InMemoryTagRepository] =
    val initCache = Map.empty[TagId, TagName]
    val initId    = 0

    Ref.Synchronized
      .make(initCache -> initId)
      .map(InMemoryTagRepository(_, cache))

@main
def threadingDepsDemo =
  Runtime.default.unsafeRun(
    for
      logger  <- ZIO.succeed(new ConsoleLogger)
      cache1  <- TagInMemoryCache.make(logger)
      cache2  <- TagInMemoryCache.make(logger) // not used, just to demonstrate boilerplate
      cache3  <- TagInMemoryCache.make(logger) // not used, just to demonstrate boilerplate
      tagRepo <- InMemoryTagRepository.make(cache1)
      id1     <- tagRepo.create(TagName.fromString("alpha"))
      id2     <- tagRepo.create(TagName.fromString("beta"))
      id3     <- tagRepo.create(TagName.fromString("gamma"))
      _       <- tagRepo.delete(id2)
      tag1    <- tagRepo.find(id1)
      _       <- logger.info(s"Got $tag1")
      tag2    <- tagRepo.find(id2)
      _       <- logger.info(s"Got $tag2")
    yield ()
  )

/**
 * Implicits to the rescue?
 */
class ImplicitTagInMemoryCache(
  ref: Ref[(Map[TagId, TagName], List[TagId])]
)(using
  log: Logger
) extends TagInMemoryCache(ref, log)

object ImplicitTagInMemoryCache:
  def make(using logger: Logger): UIO[TagInMemoryCache] =
    TagInMemoryCache.make(logger)

class ImplicitInMemoryTagRepository(
  ref: Ref.Synchronized[(Map[TagId, TagName], Int)]
)(using
  cache: Cache[TagId, TagName]
) extends InMemoryTagRepository(ref, cache)

object ImplicitInMemoryTagRepository:
  def make(using cache: Cache[TagId, TagName]): UIO[InMemoryTagRepository] =
    InMemoryTagRepository.make(cache)

@main
def implicitInjectionDemo =
  Runtime.default.unsafeRun(
    for
      _                           <- ZIO.unit
      given Logger                 = new ConsoleLogger
      given Cache[TagId, TagName] <- ImplicitTagInMemoryCache.make
      cache2                      <- ImplicitTagInMemoryCache.make // not used, just to demonstrate no passing around logger
      cache3                      <- ImplicitTagInMemoryCache.make // not used, just to demonstrate no passing around logger
      tagRepo                     <- ImplicitInMemoryTagRepository.make
      id1                         <- tagRepo.create(TagName.fromString("alpha"))
      id2                         <- tagRepo.create(TagName.fromString("beta"))
      id3                         <- tagRepo.create(TagName.fromString("gamma"))
      _                           <- tagRepo.delete(id2)
      tag1                        <- tagRepo.find(id1)
      _                           <- summon[Logger].info(s"Got $tag1")
      tag2                        <- tagRepo.find(id2)
      _                           <- summon[Logger].info(s"Got $tag2")
    yield ()
  )

/**
 * While implicits save us from passing around values at function application site,
 * we still have to declare all the required implicits in scope.
 *
 * Also each implicit parameter has to be defined as a seperate input, we can't simply pass
 * a single "Environment" that contains everything the program needs to run.
 *
 * Another issue is that there can be only one implicit value of a given type in scope, but
 * as in our example above, we might have multiple instances. For example we may have
 * two diffrent ExecutionContexts for blocking and non-blocking operations.
 */

@main
def zioEnvDemo =

  val program =
    for
      n <- Random.nextIntBetween(1, 65)
      _ <- Console.printLine(s"Random number of the day is $n")
    yield ()

  Runtime.default.unsafeRun(program)

// Zio access helpers
