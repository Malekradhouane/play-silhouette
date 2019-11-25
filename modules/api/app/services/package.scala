package co.audasoft.immo

import org.joda.time.DateTime
import play.api.libs.json.{ JodaReads, JodaWrites, Reads, Writes }

package object services {

  implicit val objectIdFormat = reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

  implicit val jodaDateTimeReads: Reads[DateTime] = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  implicit val jodaDateTimeWrites: Writes[DateTime] = JodaWrites.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
}
