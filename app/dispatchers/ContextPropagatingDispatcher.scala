package dispatchers

import java.util.concurrent.TimeUnit

import akka.dispatch._
import com.typesafe.config.Config
import org.slf4j.MDC
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Configurator for a context propagating dispatcher.
 */
class ContextPropagatingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new ContextPropagatingDisptacher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    FiniteDuration(config.getDuration("throughput-deadline-time", TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS),
    configureExecutor(),
    FiniteDuration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}

/**
 * A context propagating dispatcher.
 * 
 * This dispatcher propagates the current request context if it's set when it's executed.
 */
class ContextPropagatingDisptacher(_configurator: MessageDispatcherConfigurator,
                                    id: String,
                                    throughput: Int,
                                    throughputDeadlineTime: Duration,
                                    executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
                                    shutdownTimeout: FiniteDuration) extends Dispatcher(
  _configurator, id, throughput, throughputDeadlineTime, executorServiceFactoryProvider, shutdownTimeout
) { self =>

  override def prepare(): ExecutionContext = new ExecutionContext {
    // capture the context
    val context = RequestContext.capture()

    def execute(r: Runnable) = self.execute(new Runnable {
      // Run the runnable with the captured context
      def run() = context.withContext(r.run())
    })
    def reportFailure(t: Throwable) = self.reportFailure(t)
  }
}

/**
 * The current request context.
 */
object RequestContext {
  private val request = new ThreadLocal[RequestHeader]()

  private def setRequest(rh: RequestHeader) = {
    request.set(rh)
    MDC.put("requestId", rh.id.toString)
  }

  private def clear() = {
    request.remove()
    MDC.clear()
  }

  /**
   * Capture the current request context.
   */
  def capture(): CapturedRequestContext = new CapturedRequestContext {
    val maybeRequest = getRequest
    def withContext[T](block: => T) = maybeRequest match {
      case Some(rh) => withRequest(rh)(block)
      case None => block
    }
  }

  /**
   * Get the current request ID.
   */
  def getRequest: Option[RequestHeader] = Option(request.get())

  /**
   * Execute the given block with the given request id.
   */
  def withRequest[T](rh: RequestHeader)(block: => T) = {
    assert(rh != null, "RequestHeader must not be null")

    val maybeOld = getRequest
    try {
      setRequest(rh)
      block
    } finally {
      maybeOld match {
        case Some(old) => setRequest(old)
        case None => clear()
      }
    }
  }
}

/**
 * A captured request context
 */
trait CapturedRequestContext {

  /**
   * Execute the given block with the captured request context.
   */
  def withContext[T](block: => T)
}
