package filters

import dispatchers.RequestContext
import play.api.mvc.{RequestHeader, EssentialAction, EssentialFilter}

/**
 * Filter that sets the context
 */
object ContextSettingFilter extends EssentialFilter {
  def apply(next: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = RequestContext.withRequest(rh)(next(rh))
  }
}
