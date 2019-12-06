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

  def findAll()(implicit reads: Reads[T]): Future[Seq[T]] = collection.flatMap { coll =>
    coll.find(Json.obj()).sort(defaultSort).cursor[T](ReadPreference.nearest).collect[Seq](0, Cursor.FailOnError[Seq[T]]())
  }

  def findById(id: String)(implicit reads: Reads[T]): Future[Option[T]] = collection.flatMap { coll =>
    logger.trace(s"find $collectionName by id: $id")
    coll.find(BSONDocument("_id" -> BSONObjectID.parse(id).get)).one[T] // FIXME
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
