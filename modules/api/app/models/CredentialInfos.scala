package models

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import play.api.libs.json.Json

case class CredentialInfos(
  loginInfo: LoginInfo,
  authInfo: PasswordInfo
)

object CredentialInfos {
  implicit lazy val passwordInfoFormat = Json.format[PasswordInfo]
  implicit lazy val credentialInfosFormat = Json.format[CredentialInfos]
}
