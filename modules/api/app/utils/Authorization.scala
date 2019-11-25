package utils

import models.Role.Role
import models.User
import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import play.api.mvc.Request

import scala.concurrent.Future

/**
 * Only allows those users that have at least a role of the selected.
 * Master role is always allowed.
 * Ex: WithRole("roleA", "roleB") => only users with roles "roleA" OR "roleB" (or "master") are allowed.
 */
case class WithService(anyOf: Role*) extends Authorization[User, JWTAuthenticator] {
  def isAuthorized[A](user: User, authenticator: JWTAuthenticator)(implicit r: Request[A]) = Future.successful {
    WithService.isAuthorized(user, anyOf.map(_.toString): _*)
  }
}

object WithService {
  def isAuthorized(user: User, anyOf: String*): Boolean =
    anyOf.intersect(user.roles).nonEmpty || user.roles.contains("root")
}

/**
 * Only allows those users that have every of the selected services.
 * Master service is always allowed.
 * Ex: Restrict("serviceA", "serviceB") => only users with services "serviceA" AND "serviceB" (or "master") are allowed.
 */
case class WithServices(allOf: Role*) extends Authorization[User, JWTAuthenticator] {
  def isAuthorized[A](user: User, authenticator: JWTAuthenticator)(implicit r: Request[A]) = Future.successful {
    WithServices.isAuthorized(user, allOf.map(_.toString): _*)
  }
}

object WithServices {
  def isAuthorized(user: User, allOf: String*): Boolean =
    allOf.intersect(user.roles).size == allOf.size || user.roles.contains("root")
}