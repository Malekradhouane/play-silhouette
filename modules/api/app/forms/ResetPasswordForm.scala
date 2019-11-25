package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * The form which handles the sign up process.
 */
object ResetPasswordForm {

  /**
   * A play framework form.
   */
  val form = Form(
    mapping(
      "email" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  /**
   *
   * @param email
   */
  case class Data(
    email: String,

  )

  /**
   * The companion object.
   */
  object Data {

    /**
     * Converts the [Date] object to Json and vice versa.
     */
    implicit val resetPasswordDataFormat = Json.format[Data]
  }
}
