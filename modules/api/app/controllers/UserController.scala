package controllers

import akka.actor.Status.Success
import akka.http.javadsl.model.headers.Authorization
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.{Accept, Authorization, GenericHttpCredentials}
import cats.Id
import conf.AppConfig
import forms._
import models.User
import repositories.MongoDBAuthInfoRepository
import services.UserService
import utils.{AuthController, MyEnv, WithService}
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{Clock, Credentials, PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent, Silhouette}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.{CredentialsProvider, SocialProviderRegistry}
import com.nimbusds.jwt.JWT
import com.typesafe.config.Config
import io.jsonwebtoken.Jwt
import javax.inject.{Inject, Singleton}
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
case class UserController @Inject()(
                                     userService: UserService,
                                     silhouette: Silhouette[MyEnv],
                                     authInfoRepository: AuthInfoRepository,
                                     credentialsProvider: CredentialsProvider,
                                     socialProviderRegistry: SocialProviderRegistry,
                                     passwordHasher: PasswordHasher,
                                     configuration: Configuration,
                                     appConfig: AppConfig,
                                     clock: Clock,
                                     avatarService: AvatarService,
                                     mongoDBAuthInfoRepository: MongoDBAuthInfoRepository
                                   ) extends InjectedController with AuthController with I18nSupport {

  import models.Role._
  import models.UserFormat._

  def newUser = SecuredAction(WithService(userWrite)).async(parse.json) { request =>
    val user = request.body.as[User]
    userService.newUser(user).map(_ => Created)

  }

  def getUser(accountId: String) = Action.async { request =>
    userService.getUser(accountId).map {
      case Some(account) => Ok(Json.toJson(account))
      case _ => NotFound
    }
  }

  def getCurrentUser() = SecuredAction(WithService(userRead)).async { request =>
    val loginInfo = request.authenticator.loginInfo
    userService.retrieve(loginInfo).map {
      case Some(user) => Ok(
        Json.obj("user" -> user)
      )
      case _ => NotFound
    }
  }

  def getUsers() = SecuredAction(WithService(userRead)).async { request => // FIXME For admin only
    userService.getAllUsers().map { users =>
      Ok(Json.toJson(users))
    }.recover {
      case NonFatal(e) =>
        e.printStackTrace()
        InternalServerError
    }
  }

  def updateAccount(userId: String): Action[JsValue] = SecuredAction(WithService(userWrite)).async(parse.json) {
    implicit request =>
      userService.updateUser(userId, request.body.as[User]).map(_ => Created)
  }

  def deleteAccount(userId: String) = SecuredAction(WithService(userDelete)).async { implicit request =>
    userService.getUser(userId).flatMap {
      case Some(user) =>
        for {
          _ <- mongoDBAuthInfoRepository.remove(user.loginInfo)
          _ <- userService.deleteUser(userId)
        } yield {
          Ok("user deleted Successfully")
        }
      case None => Future.successful(
        Unauthorized(Json.obj("message" -> Messages("invalid.data")))
      )
    }
  }

  def signUp() = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUpForm.Data].map { data =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email.toLowerCase)

      userService.retrieve(loginInfo).flatMap {
        case Some(_) =>
          Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
        case None =>
          val authInfo: PasswordInfo = passwordHasher.hash(data.password)

          val user = User(
            _id = Some(BSONObjectID.generate()),
            loginInfo = loginInfo,
            firstName = data.firstName,
            lastName = data.lastName,
            fullName = computeFullName(data.firstName, data.lastName),
            email = data.email.toLowerCase,
            roles = newUserRoles,
            activated = false // user need to verify account with verificationCode sent by email
          )

          for {

            avatar <- avatarService.retrieveURL(data.email.toLowerCase)
            _ <- userService.newUser(user.copy(avatarURL = avatar), Some(data))
            authInfo <- authInfoRepository.add(loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(loginInfo)
            token <- silhouette.env.authenticatorService.init(authenticator)


          } yield {

            silhouette.env.eventBus.publish(SignUpEvent(user, request))
            silhouette.env.eventBus.publish(LoginEvent(user, request))

            Ok(Json.obj(
              "token" -> token,
              "userId" -> user._id.get.stringify,
              "activated" -> user.activated
            ))
          }
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.data"))))
    }
  }

  /**
    * Handles the submitted JSON data.
    *
    * @return The result to display.
    */
  def signIn = Action.async(parse.json) { implicit request =>

    request.body.validate[SignInForm.Data].map { data =>
      credentialsProvider.authenticate(Credentials(data.email.toLowerCase, data.password)).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => silhouette.env.authenticatorService.create(loginInfo).map {
            case authenticator if data.rememberMe =>
              val c: Config = configuration.underlying
              authenticator.copy(
                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
              )
            case authenticator => authenticator
          }.flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            silhouette.env.authenticatorService.init(authenticator).map { token =>
              Ok(Json.obj(
                "token" -> token,
                "userId" -> user._id.get.stringify,
                "activated" -> user.activated,
              ))
            }
          }
          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
        }
      }.recover {
        case e: ProviderException =>
          Unauthorized(Json.obj("message" -> Messages("invalid.credentials")))
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.credentials"))))
    }
  }

  /**
    * Saves the new password and renew the cookie
    */
  def changePassword = SecuredAction(WithService(userWrite)).async(parse.json) { implicit request =>

    request.body.validate[ChangePasswordForm.Data].map { data =>
      val credentials = Credentials(data.email.toLowerCase, data.currentPassword)

      val authInfo: PasswordInfo = passwordHasher.hash(data.newPassword)

      credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            for {
              authInfo <- mongoDBAuthInfoRepository.update(loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              result <- silhouette.env.authenticatorService.renew(authenticator, Ok("success"))


            } yield {
              result

            }

          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
        }
      }
    }.recoverTotal {
      case error =>
        Future.successful(BadRequest(Json.obj("message" -> "error changing user password")))
    }
  }

  def resetPassword() = Action.async(parse.json) { implicit request =>
    request.body.validate[ResetPasswordForm.Data].map { data =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email.toLowerCase)
      userService.retrieve(loginInfo).flatMap {
        case None =>
          Future.successful(BadRequest(Json.obj("message" -> Messages("Aucun compte n’existe avec cette adresse email"))))

        case Some(user) =>
          for {
            avatar <- avatarService.retrieveURL(data.email.toLowerCase)

            authenticator <- silhouette.env.authenticatorService.create(loginInfo)

            token <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.renew(authenticator, Ok("success"))


          } yield {
            Ok(Json.obj("token" -> token))
          }
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.data"))))
    }
  }

  def newPassword = SecuredAction(WithService(userWrite)).async(parse.json) { implicit request =>

    request.body.validate[NewPasswordForm.Data].map { data =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email.toLowerCase)
      val authInfo: PasswordInfo = passwordHasher.hash(data.newPassword)

      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          for {

            _ <- authInfoRepository.update(loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(loginInfo)
            result <- silhouette.env.authenticatorService.renew(authenticator, Ok("success"))
          } yield {

            result
          }
      }
    }.recoverTotal {
      case error =>
        Future.successful(BadRequest(Json.obj("message" -> "error changing user password")))
    }
  }

  /**
    * Verify user account with verificationCode
    *
    * @param verificationCode
    * @param email
    * @return
    */
  def verifyAccount(email: String) = SecuredAction(WithService(userWrite)).async(parse.json) { request =>
    val loginInfo = LoginInfo(CredentialsProvider.ID, email.toLowerCase)
    userService.retrieve(loginInfo).flatMap {

      case Some(user) =>
        for {

          _ <- userService.updateUser(user._id.get.stringify, user.copy(activated = true, roles = activatedAccount))


        } yield {

          Ok(Json.obj("user" -> user.copy(activated = true)))

        }

    }
  }

  def resendVerificationEmail(email: String) = Action.async(parse.json) { implicit request =>
    val loginInfo = LoginInfo(CredentialsProvider.ID, email.toLowerCase)
    userService.retrieve(loginInfo).flatMap {

      case Some(user) =>
        for {
          avatar <- avatarService.retrieveURL(email.toLowerCase)

          authenticator <- silhouette.env.authenticatorService.create(loginInfo)

          token <- silhouette.env.authenticatorService.init(authenticator)
          result <- silhouette.env.authenticatorService.renew(authenticator, Ok("success"))


        } yield {
          Ok("email was sent Successfully")

        }

    }
  }

  def changeEmail() = SecuredAction(WithService(userWrite)).async(parse.json) { implicit request =>
    request.body.validate[ChangeEmailForm.Data].map { data =>
      val credentials = Credentials(data.currentEmail.toLowerCase, data.password)
      val authInfo: PasswordInfo = passwordHasher.hash(data.password)
      credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case None =>
            Future.successful(BadRequest(Json.obj("message" -> Messages("email not found"))))

          case Some(user) =>

            val loginInfo = LoginInfo(CredentialsProvider.ID, data.newEmail.toLowerCase)

            userService.retrieve(loginInfo).flatMap {
              case Some(_) =>
                Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
              case None =>

                for {
                  _ <- mongoDBAuthInfoRepository.remove(LoginInfo(CredentialsProvider.ID, data.currentEmail.toLowerCase))

                  _ <- avatarService.retrieveURL(data.newEmail)
                  _ <- userService.updateUser(user._id.get.stringify, user.copy(activated = false, loginInfo = loginInfo, email = data.newEmail))

                  authInfo <- authInfoRepository.add(loginInfo, authInfo)
                  authenticator <- silhouette.env.authenticatorService.create(loginInfo)
                  token <- silhouette.env.authenticatorService.init(authenticator)


                  result <- silhouette.env.authenticatorService.renew(authenticator, Ok("success"))


                } yield result
            }
          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
        }
      }
    }.recoverTotal {
      case error =>
        Future.successful(BadRequest(Json.obj("message" -> "error changing user email")))
    }
  }

  private def computeFullName(firstName: Option[String], lastName: Option[String]): Option[String] = {
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

