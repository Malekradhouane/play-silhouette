package utils

import models.User
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import play.api.i18n.I18nSupport
import play.api.mvc.{ AnyContent, InjectedController }

trait AuthController extends InjectedController with I18nSupport {
  def silhouette: Silhouette[MyEnv]
  def env: Environment[MyEnv] = silhouette.env

  def SecuredAction: SecuredActionBuilder[MyEnv, AnyContent] = silhouette.SecuredAction
  def UnsecuredAction = silhouette.UnsecuredAction
  def UserAwareAction = silhouette.UserAwareAction

  implicit def securedRequest2User[A](implicit request: SecuredRequest[MyEnv, A]): User = request.identity
  implicit def userAwareRequest2UserOpt[A](implicit request: UserAwareRequest[MyEnv, A]): Option[User] = request.identity
}