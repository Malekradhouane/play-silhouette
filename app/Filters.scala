import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter

case class Filters @Inject() (cORSFilter: CORSFilter) extends HttpFilters {
  override def filters = Seq(cORSFilter)
}
