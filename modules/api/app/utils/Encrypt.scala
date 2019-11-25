package utils

import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Hex

object Encrypt {

  import java.security.MessageDigest

  def md5(s: String): String = {
    val bytes = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"))

    new String(Hex.encodeHex(bytes))
  }

}
