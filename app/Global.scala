import filters.ContextSettingFilter
import play.api.GlobalSettings
import play.api.mvc.WithFilters

object Global extends WithFilters(ContextSettingFilter) with GlobalSettings
