package models

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

import scala.util.Try


case class User(
    _id: Option[BSONObjectID] = None,
    loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    fullName: Option[String],
    email: String,
    roles: Seq[String] = Seq.empty[String],
    activated: Boolean = false,
    avatarURL: Option[String] = None,
    phoneNumber: Option[String] = None,
    creationDate: Option[DateTime] = Some(DateTime.now()),
    deletionDate: Option[DateTime] = None
) extends Identity {

}

object User {
  val emptyUser = User(
    _id = None,
    loginInfo = LoginInfo("", ""),
    firstName = None,
    lastName = None,
    fullName = None,
    email = "",
  )
}

object UserFormat {

  import json.DateTimeJsonFormat._
  import json.ObjectIdJsonFormat._

  implicit val jsonFormat = Json.format[User]
}

object UserBSONFormat {

  import json.DateTimeJsonFormat._
  import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

  implicit val jsonFormat = Json.format[User]
}

object Role extends Enumeration {
  type Role = Value
  val userRead, userWrite, userDelete = Value

  val root = Value

  val rootRoles: Seq[String] = Seq(
    userRead, userWrite, userDelete
  ).map(_.toString)



  val newUserRoles = Seq(
    userRead, userWrite,

  ).map(_.toString)


}

