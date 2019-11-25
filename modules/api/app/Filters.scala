import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter
import play.filters.hosts.AllowedHostsFilter

case class Filters @Inject() (cORSFilter: CORSFilter, allowedHostsFilter: AllowedHostsFilter) extends DefaultHttpFilters(
  cORSFilter,
  allowedHostsFilter
)
