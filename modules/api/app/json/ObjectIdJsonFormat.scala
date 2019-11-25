package json

import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

object ObjectIdJsonFormat {

  implicit val bSONObjectIDReads = new Reads[BSONObjectID] {
    override def reads(json: JsValue): JsResult[BSONObjectID] = {

      import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
      JsSuccess(BSONObjectID.parse(json.as[String]).get) //FIXME
    }
  }

  implicit val bSONObjectIDWrites = new Writes[BSONObjectID] {
    def writes(bSONObjectID: BSONObjectID) = JsString(bSONObjectID.stringify)
  }
}

