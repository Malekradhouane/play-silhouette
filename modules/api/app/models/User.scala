package models

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

import scala.util.Try

object UserType extends Enumeration {
  type UserType = Value
  val Seller, Buyer, Agency, Sponsor = Value
}

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
  val propertyRead, propertyWrite, propertyDelete = Value
  val propertyTypeRead, propertyTypeWrite, propertyTypeDelete = Value
  val promotionRead, promotionWrite, promotionDelete = Value
  val planRead, planWrite, planDelete = Value
  val messageRead, messageWrite, messageDelete = Value
  val billRead, billWrite, billDelete = Value
  val amenityRead, amenityWrite, amenityDelete = Value
  val alertRead, alertWrite, alertDelete = Value
  val agencyUserRead, agencyUserWrite, agencyUserDelete = Value
  val agencyRead, agencyWrite, agencyDelete = Value
  val groupRead, groupWrite, groupDelete = Value
  val locationRead, locationWrite, locationDelete = Value
  val reviewRead, reviewWrite, reviewDelete = Value
  val favoriteRead, favoriteWrite, favoriteDelete = Value
  val topicRead, topicWrite, topicDelete = Value
  val articleRead, articleWrite, articleDelete = Value
  val adminRead, adminWrite, adminDelete = Value
  val notificationWrite, notificationRead, notificationDelete = Value
  val root = Value

  val rootRoles: Seq[String] = Seq(
    propertyRead, propertyWrite, propertyDelete,
    groupRead, groupWrite, groupDelete
  ).map(_.toString)

  val propertyAdministratorRoles: Seq[String] = Seq(
    propertyRead, propertyWrite,
    groupRead
  ).map(_.toString)

  val guestRoles: Seq[String] = Seq(
    propertyRead, agencyRead
  ).map(_.toString)

  val newUserRoles = Seq(
    propertyRead,
    agencyRead,
    userRead, userWrite,
    agencyUserRead,
    propertyTypeRead,
    promotionRead,
    planRead,
    messageRead,
    alertRead,
    locationRead,
    reviewRead,
    amenityRead,
  ).map(_.toString)

  val agencyUserRoles = Seq(
    propertyRead, propertyWrite, propertyDelete,
    agencyRead,
    userRead, userWrite,
    agencyUserRead, agencyUserWrite, agencyUserDelete,
    propertyTypeRead,
    promotionRead,
    planRead,
    messageRead, messageWrite, messageWrite,
    alertRead, alertWrite, alertDelete,
    locationRead,
    reviewRead, reviewWrite, reviewDelete,
    amenityRead,
  ).map(_.toString)

  val activatedAccount = Seq(
    propertyRead, propertyWrite, propertyDelete,
    agencyRead,
    userRead, userWrite,
    agencyUserRead,
    propertyTypeRead,
    messageRead, messageWrite, messageWrite,
    alertRead, alertWrite, alertDelete,
    locationRead,
    reviewRead, reviewWrite, reviewDelete,
    amenityRead,
  ).map(_.toString)

}

