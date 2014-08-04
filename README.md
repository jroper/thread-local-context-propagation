Thread local context propagation example
========================================

This project shows an example of thread local context propagation.  It captures the request in a thread local, making
it available to any code executed by the Play default execution context.  It also sets the request id in the SLF4J
MDC, so that the request id can be used in the logs.

A few caveats:

* It doesn't work properly in dev mode.  On first load, it will work, subsequent loads won't work properly, since the
  dispatcher is loaded once, and is used for every subsequent reload, even though the rest of the Play application
  is reloaded.  Hence it will only work when the application is first loaded.  This won't be a problem in prod.  To
  work around this, extract this code into an external library, and bring it onto the classpath like normal dependency.
* I don't recommend doing this.  It has performance implications, and in an asynchronous application, it can be very
  difficult to track what is done on what thread - you don't want to have to worry about that, you want the framework
  to worry about that for you, but transferring state on thread locals forces you to always have this implicit
  requirement in your mind as you code.  In my opinion, this just isn't worth the pain.  Pass state/context explicitly,
  then everything becomes very easy to understand, test, and reason about.
  
So, with those things in mind, make sure you always use the Play default execution context if you want this to work.

If you do use other execution contexts, make sure they are Akka dispatchers, configured with the appropriate `type`
as shown in `application.conf`.

Also, this won't work with actors, since actors don't call the prepare method - with actors you have no choice but to
explicitly pass the context.  Though, usually in the actor model, there is no one to one relationship from messages to
requests, so passing request context doesn't make sense.