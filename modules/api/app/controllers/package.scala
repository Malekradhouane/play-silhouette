

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.password.BCryptPasswordHasher

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import scala.language.implicitConversions

package object controllers {
  implicit val bSONObjectIDReads = new Reads[BSONObjectID] {
    override def reads(json: JsValue): JsResult[BSONObjectID] = {

      JsSuccess(BSONObjectID.parse(json.as[String]).get) //FIXME
    }
  }

  implicit val bSONObjectIDWrites = new Writes[BSONObjectID] {
    def writes(bSONObjectID: BSONObjectID) = JsString(bSONObjectID.stringify)
  }

  implicit val jodaDateTimeReads: Reads[DateTime] = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  implicit val jodaDateTimeWrites: Writes[DateTime] = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")

  implicit def key2loginInfo(key: String): LoginInfo = LoginInfo(CredentialsProvider.ID, key)
  implicit def loginInfo2key(loginInfo: LoginInfo): String = loginInfo.providerKey
  implicit def pwd2passwordInfo(pwd: String): PasswordInfo = PasswordInfo(BCryptPasswordHasher.ID, pwd, salt = Some("your-salt"))
  implicit def passwordInfo2pwd(passwordInfo: PasswordInfo): String = passwordInfo.password
}
