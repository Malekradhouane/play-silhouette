package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
  * The form which handles the submission of the credentials.
  */
object ChangeEmailForm {

  /**
    * A play framework form.
    */
  val form = Form(
    mapping(
      "currentEmail" -> email,
      "newEmail" -> email,
      "password" -> nonEmptyText,
    )(Data.apply)(Data.unapply)
  )

  /**
    * The form data.
    *
    * @param email    The email of the user.
    * @param password The password of the user.
    */
  case class Data(
                   currentEmail: String,
                   newEmail: String,
                   password: String,
                 )

  object Data {
    implicit val signInDataFormat = Json.format[Data]
  }

}
