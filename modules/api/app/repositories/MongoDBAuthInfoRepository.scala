package repositories

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import models.{ CredentialInfos, User }
import services.UserService
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import org.slf4j.LoggerFactory
import play.api.i18n.Messages
import play.api.libs.json.{ Json, OWrites }
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.bson.{ BSONDocument, BSONObjectID }
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import scala.concurrent.{ Await, Future }

case class MongoDBAuthInfoRepository @Inject() (
    reactiveMongoApi: ReactiveMongoApi

) extends DelegableAuthInfoDAO[PasswordInfo] with AbstractRepository[User] {

  import reactivemongo.play.json._
  import models.UserBSONFormat._

  val logger = LoggerFactory.getLogger(this.getClass)

  val collectionName = "credentialInfos"

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = collection.flatMap { coll =>
    coll.find(
      Json.obj(
        "loginInfo.providerID" -> loginInfo.providerID,
        "loginInfo.providerKey" -> loginInfo.providerKey
      )
    ).one[CredentialInfos].map(_.map(_.authInfo))
  }

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = collection.flatMap { coll =>
    coll.find(
      Json.obj(
        "loginInfo.providerID" -> loginInfo.providerID,
        "loginInfo.providerKey" -> loginInfo.providerKey
      )
    ).one[User]
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    collection.flatMap { coll =>
      coll.insert(CredentialInfos(loginInfo, authInfo))
    }.map {
      case result if result.ok => authInfo
      case result if !result.ok => throw new Exception(result.writeErrors.toString())
    }
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val selector = BSONDocument("loginInfo.providerKey" -> loginInfo.providerKey)

    collection.flatMap { coll =>
      coll.find(
        Json.obj(
          "loginInfo.providerID" -> loginInfo.providerID,
          "loginInfo.providerKey" -> loginInfo.providerKey
        )
      ).one[CredentialInfos].flatMap {
          case Some(_) => coll.update(selector, CredentialInfos(loginInfo, authInfo)).map {
            case result if result.ok => authInfo

            case result if !result.ok => throw new Exception(result.writeErrors.toString())
          }

          case None => add(loginInfo, authInfo)
        }
    }

  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo) = collection.flatMap { coll =>
    val selector = BSONDocument("loginInfo.providerKey" -> loginInfo.providerKey)
    coll.remove(selector)
    Future.successful(())
  }
}