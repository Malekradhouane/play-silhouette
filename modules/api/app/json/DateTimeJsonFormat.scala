package json

import org.joda.time.DateTime
import play.api.libs.json.{ JodaReads, JodaWrites }

object DateTimeJsonFormat {
  import play.api.libs.json.{ Reads, Writes }

  implicit val jodaDateTimeReads: Reads[DateTime] = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  implicit val jodaDateTimeWrites: Writes[DateTime] = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")

}