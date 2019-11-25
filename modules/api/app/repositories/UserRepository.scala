package repositories

import models.User
import com.mohiva.play.silhouette.api.LoginInfo
import javax.inject.{ Inject, Singleton }
import jdk.nashorn.internal.runtime.options.LoggingOption.LoggerInfo
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
case class UserRepository @Inject() (reactiveMongoApi: ReactiveMongoApi) extends AbstractRepository[User] {

  val logger = LoggerFactory.getLogger(this.getClass)
  import reactivemongo.play.json._
  import models.UserBSONFormat._
  import LoggerInfo._

  val collectionName = "users"

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = collection.flatMap { coll =>
    coll.find(
      Json.obj(
        "loginInfo.providerID" -> loginInfo.providerID,
        "loginInfo.providerKey" -> loginInfo.providerKey
      )
    ).one[User]
  }

}
