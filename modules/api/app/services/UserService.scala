package services

import conf.AppConfig
import forms.{ SignUpForm }
import models.User
import repositories.UserRepository
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{ AvatarService, IdentityService }
import com.mohiva.play.silhouette.api.util.{ PasswordHasher, PasswordInfo }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import javax.inject.{ Inject, Singleton }
import org.slf4j.LoggerFactory
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserService @Inject() (
    userRepository: UserRepository,
    passwordHasher: PasswordHasher,
    avatarService: AvatarService,
    authInfoRepository: AuthInfoRepository,
    appConfig: AppConfig
) extends IdentityService[User] {
  val logger = LoggerFactory.getLogger(getClass)

  import models.UserBSONFormat._

  def newUser(user: User, signUpData: Option[SignUpForm.Data] = None) = {
    logger.trace(s"insert new user: $user")
    userRepository.insert(user)
  }
  def newUser(user: User) = {
    logger.trace(s"insert new user: $user")
    userRepository.insert(user)
  }
  def getAllUsers() = {
    logger.trace(s"get All users")
    userRepository.findAll()
  }

  def getUser(userId: String) = {
    logger.trace(s"get user with id $userId")
    if (userId.nonEmpty) {
      userRepository.findById(userId: String)
    } else {
      Future.successful(None)
    }
  }

  def deleteUser(userId: String) = {
    logger.trace(s"delete user with id $userId")
    userRepository.remove(userId)
  }

  def updateUser(userId: String, user: User) = {
    logger.trace(s"update user: $user")

    userRepository.update(BSONObjectID.parse(userId).get, user.copy(fullName = computeFullName(user.firstName, user.lastName)))
  }

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    userRepository.retrieve(loginInfo)
  }

  def computeFullName(firstName: Option[String], lastName: Option[String]): Option[String] = {
    if (firstName.isDefined && lastName.isDefined) {
      Some(firstName.getOrElse("") + " " + lastName.getOrElse(""))
    } else {
      if (firstName.isDefined) {
        firstName
      } else if (lastName.isDefined) {
        lastName
      } else {
        None
      }
    }
  }
}
