package utils

import scala.util.Random

object PasswordGenerator {

  def randomPassword(): String = {
    val randomPassword: Random = new scala.util.Random(31)
    randomPassword.nextString(10)
  }

}
