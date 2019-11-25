package repositories

import cats.data.{ EitherT, NonEmptyList, OptionT }
import error.{ AppError, MongodbError }
import org.joda.time.DateTime
import org.slf4j.Logger
import play.api.libs.json.{ JsObject, Json, OWrites, Reads }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AbstractRepository[T] {

  import reactivemongo.api._
  import reactivemongo.bson._
  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
  import reactivemongo.play.json._

  def logger: Logger

  def collection: Future[JSONCollection] = reactiveMongoApi.database.map(db => db.collection[JSONCollection](collectionName))
  def reactiveMongoApi: ReactiveMongoApi
  def collectionName: String
  def defaultSort = Json.obj()

  def insertF(t: T)(implicit writes: OWrites[T]): EitherT[Future, AppError, T] = {
    logger.trace(s"insert new document into $collectionName: $t, ${writes.writes(t)}")
    EitherT(collection.flatMap { coll =>
      coll.insert(t)
    }.map {
      case result if result.ok => Right(t)
      case result => Left(MongodbError(result.writeErrors.map(_.errmsg).mkString(",")))
    })
  }

  def bulkInsert(seqT: Seq[T])(implicit writes: OWrites[T]): EitherT[Future, AppError, Seq[T]] = {
    logger.trace(s"bulk into $collectionName, $seqT")
    EitherT(collection.flatMap { coll =>
      val documents = seqT.map(implicitly[coll.ImplicitlyDocumentProducer](_))

      coll.bulkInsert(ordered = false)(documents: _*)
    }.map {
      case result if result.ok => Right(seqT)
      case result => Left(MongodbError(result.writeErrors.map(_.errmsg).mkString(",")))
    })
  }

  def insert(newObject: T)(implicit writes: OWrites[T]) = {
    logger.trace(s"insert new document into $collectionName: $newObject, ${writes.writes(newObject)}")
    collection.flatMap { coll =>
      coll.insert(newObject)
    }
  }

  def update(id: BSONObjectID, newObject: T)(implicit writes: OWrites[T]): Future[UpdateWriteResult] = {
    logger.trace(s"insert new document into $collectionName: $newObject, ${writes.writes(newObject)}")
    val selector = BSONDocument(
      "_id" -> id
    )
    collection.flatMap { coll =>
      coll.update(selector, newObject, upsert = true)
    }
  }

  def updateF(id: BSONObjectID, newObject: T)(implicit writes: OWrites[T]): EitherT[Future, AppError, T] = {
    logger.trace(s"insert new document into $collectionName: $newObject, ${writes.writes(newObject)}")
    val selector = BSONDocument(
      "_id" -> id
    )
    EitherT(collection.flatMap { coll =>
      coll.update(selector, newObject, upsert = true)
    }.map {
      case result if result.ok => Right(newObject)
      case result => Left(MongodbError(result.writeErrors.map(_.errmsg).mkString(",")))
    })
  }

  def findAll()(implicit reads: Reads[T]): Future[Seq[T]] = collection.flatMap { coll =>
    coll.find(Json.obj()).sort(defaultSort).cursor[T](ReadPreference.nearest).collect[Seq](0, Cursor.FailOnError[Seq[T]]())
  }

  def findByIdF(id: String)(implicit reads: Reads[T]): OptionT[Future, T] = OptionT(collection.flatMap { coll =>
    logger.trace(s"find $collectionName by id: $id")
    coll.find(BSONDocument("_id" -> BSONObjectID.parse(id).get)).one[T]
  })

  def findById(id: String)(implicit reads: Reads[T]): Future[Option[T]] = collection.flatMap { coll =>
    logger.trace(s"find $collectionName by id: $id")
    coll.find(BSONDocument("_id" -> BSONObjectID.parse(id).get)).one[T] // FIXME
  }

  def findByIdsF(ids: NonEmptyList[String])(implicit reads: Reads[T]): Future[Seq[T]] = collection.flatMap { coll =>
    logger.trace(s"find by ids: $ids")

    coll.find(
      Json.obj(

        "_id" -> Json.obj("$in" -> ids.toList.map(BSONObjectID.parse(_).get))
      )
    ).cursor[T](ReadPreference.nearest).collect[Seq](0, Cursor.FailOnError[Seq[T]]())
  }

  def findByIds(ids: Seq[String])(implicit reads: Reads[T]): Future[Seq[T]] = collection.flatMap { coll =>
    logger.trace(s"find by ids: $ids")
    if (ids.nonEmpty) {
      coll.find(
        Json.obj(
          "_id" -> Json.obj("$in" -> ids.map(BSONObjectID.parse(_).get))
        )
      ).cursor[T](ReadPreference.nearest).collect[Seq](0, Cursor.FailOnError[Seq[T]]()) // FIXME
    } else Future.successful(Seq.empty[T])
  }

  def findByIdsForProperty(propertyId: String, ids: Seq[String])(implicit reads: Reads[T]): Future[Seq[T]] = collection.flatMap { coll =>
    logger.trace(s"find by ids: $ids")
    if (ids.nonEmpty) {
      coll.find(
        Json.obj(
          "propertyId" -> propertyId,
          "_id" -> Json.obj("$in" -> ids.map(BSONObjectID.parse(_).get))
        )
      ).cursor[T](ReadPreference.nearest).collect[Seq](0, Cursor.FailOnError[Seq[T]]()) // FIXME
    } else Future.successful(Seq.empty[T])
  }

  def remove(id: String)(implicit writes: OWrites[T]) = collection.flatMap { coll =>
    val selector = BSONDocument("_id" -> BSONObjectID(id))
    coll.remove(selector)
  }

  def removeAll()(implicit writes: OWrites[T]) = collection.flatMap { coll =>
    val selector = BSONDocument()
    coll.remove(selector)
  }
  def findAllByCriteria(criteria: JsObject)(implicit reads: Reads[T]): Future[Seq[T]] = collection.flatMap { coll =>
    coll.find(criteria).cursor[T](ReadPreference.nearest).collect[Seq](0, Cursor.FailOnError[Seq[T]]())
  }
  def findOneByCriteria(criteria: JsObject)(implicit reads: Reads[T]): Future[Option[T]] = collection.flatMap { coll =>
    coll.find(criteria).one[T]
  }
}
