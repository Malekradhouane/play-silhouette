package conf

import javax.inject.Inject
import com.google.inject.Singleton
import play.api.Configuration

@Singleton
case class AppConfig @Inject() (config: Configuration) {

  val paginationPageCount = config.getInt("pagination.page.count").getOrElse(20)

  val baseUrl = config.get[String]("baseUrl")

}

