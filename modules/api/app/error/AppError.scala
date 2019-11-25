package error

trait AppError {
  def message: String
}

case class PropertyNotFound(message: String) extends AppError
case class MongodbError(message: String) extends AppError