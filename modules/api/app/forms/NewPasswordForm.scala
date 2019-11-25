package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * The form which handles the sign up process.
 */
object NewPasswordForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "email" -> nonEmptyText,
      "newPassword" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  /**
   *
   * @param email
   * @param newPassword
   */
  case class Data(
    email: String,
    newPassword: String
  )

  /**
   * The companion object.
   */
  object Data {

    /**
     * Converts the [Date] object to Json and vice versa.
     */
    implicit val newPasswordDataFormat = Json.format[Data]
  }
}
