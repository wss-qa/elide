package elide.runtime.gvm.internals.context

import com.lmax.disruptor.*
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.dsl.ProducerType
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.ExecutionInputs
import elide.runtime.gvm.cfg.GuestVMConfiguration
import elide.runtime.gvm.internals.VMProperty
import org.graalvm.polyglot.Engine
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Stream
import elide.runtime.gvm.internals.VMStaticProperty as StaticProperty
import org.graalvm.polyglot.Context as VMContext

/** TBD. */
@Singleton internal class NativeContextManagerImpl @Inject constructor (config: GuestVMConfiguration) :
  ContextManager<VMContext, VMContext.Builder> {
  private companion object {
    // Static options which are supplied to the engine.
    private val staticEngineOptions = listOf(
      StaticProperty.active("engine.BackgroundCompilation"),
      StaticProperty.active("engine.UsePreInitializedContext"),
      StaticProperty.active("engine.Compilation"),
      StaticProperty.active("engine.Inlining"),
      StaticProperty.active("engine.MultiTier"),
      StaticProperty.active("engine.Splitting"),
      StaticProperty.of("engine.Mode", "latency"),
      StaticProperty.of("engine.PreinitializeContexts", "js"),
    )

    // Size of the disruptor ring buffer for each VM executor.
    private const val ringBufferSize = 32

    // Number of retries to apply to the sleeping-wait strategy.
    private const val sleepingRetries: Int = 200

    // Default thread-sleep (in nanoseconds).
    private const val defaultSleepNs: Long = 100L
  }

  /** Stubbed output stream. */
  private class StubbedOutputStream : OutputStream() {
    companion object {
      /** Singleton instance for internal use. */
      internal val SINGLETON = StubbedOutputStream()
    }

    override fun write(b: Int): Unit = error(
      "Cannot write to stubbed stream from inside the JS VM."
    )
  }

  /** Stubbed input stream. */
  private class StubbedInputStream : InputStream() {
    companion object {
      /** Singleton instance for internal use. */
      internal val SINGLETON = StubbedInputStream()
    }

    override fun read(): Int = error(
      "Cannot read from stubbed stream from inside the JS VM."
    )
  }

  /** Implements a thread local which binds a native VM thread to an exclusive VM context. */
  private inner class VMThreadLocal : ThreadLocal<VMContext?>() {
    override fun initialValue(): VMContext? = null
  }

  /** Thread implementation for a unit of guest VM work. */
//  private inner class NativeVMThread constructor (
//    private val operation: VMContext.() -> Unit,
//  ) : Thread() {
//    /** Execution lock for this thread. */
//    private val mutex = ReentrantLock()
//
//    /** Last-seen exception. */
//    private val lastException: AtomicReference<Throwable?> = AtomicReference(null)
//
//    // Prepare to enter the VM context.
//    private fun preExecute(context: VMContext) {
//      context.enter()
//    }
//
//    // Clean up after exiting the VM context.
//    private fun postExecute(context: VMContext) {
//      context.leave()
//    }
//
//    // Lock for execution.
//    private fun withLock(op: (VMContext) -> Unit) {
//      val ctx = workerContext.get() ?: error("Failed to acquire VM context")
//      mutex.lock()
//      try {
//        op.invoke(ctx)
//      } catch (err: Throwable) {
//        lastException.set(err)
//      } finally {
//        mutex.unlock()
//      }
//    }
//
//    /**
//     * TBD.
//     */
//    override fun run(): Unit = withLock {
//      try {
//        preExecute(it)
//        operation.invoke(it)
//      } finally {
//        postExecute(it)
//      }
//    }
//  }

  /**
   * TBD.
   */
  internal inner class NativeVMInvocation<Inputs: ExecutionInputs> : ContextManager.VMInvocation<Inputs> {
    // Nothing at this time.
  }

  /**
   * TBD.
   */
  private inner class VMInvocationFactory<Inputs: ExecutionInputs> : EventFactory<NativeVMInvocation<Inputs>> {
    /** @inheritDoc */
    override fun newInstance(): NativeVMInvocation<Inputs> = NativeVMInvocation()
  }

  /**
   * TBD.
   */
  private inner class NativeVMExecutor<I: ExecutionInputs> :
    EventHandler<NativeVMInvocation<I>>,
    LifecycleAware,
    AutoCloseable {
    /** Last-seen exception. */
    private val threadId: AtomicLong = AtomicLong(-1)

    /** Last-seen exception. */
    private lateinit var threadName: String

    /** Execution lock for this thread. */
    private val mutex = ReentrantLock()

    /** Last-seen exception. */
    private val lastException: AtomicReference<Throwable?> = AtomicReference(null)

    // Initialize VM context for this executor.
    @Suppress("DEPRECATION") private fun initializeVMContext() {
      val thread = Thread.currentThread()
      logging.debug { "Allocating VM context for thread '${thread.name}'" }
      val ctx = allocateContext()
      logging.trace { "Context ready for VM thread '${thread.name}'" }
      workerContext.set(ctx)
      threadId.set(thread.id)
      threadName = thread.name
      logging.trace { "VM worker initialized for thread '${thread.name}'" }
    }

    /** @inheritDoc */
    override fun close() {
      try {
        logging.debug("Closing executor context: $threadName")
        val ctx = workerContext.get()
        if (ctx == null) {
          logging.warn("Context already de-allocated")
          return
        }
        ctx.close(true)
        logging.trace("VM context closed: $threadName")
      } catch (err: Throwable) {
        logging.error("Unhandled exception while closing VM context", err)
      }
    }

    /** @inheritDoc */
    override fun onStart() {
      try {
        initializeVMContext()
      } catch (err: Throwable) {
        error("Failed to initialize VM context: $err")
      }
    }

    /** @inheritDoc */
    override fun onShutdown() = close()

    // Prepare to enter the VM context.
    private fun preExecute(context: VMContext) {
      if (logging.isEnabled(LogLevel.TRACE))
        logging.trace("Entering VM execution context: $threadName")
      context.enter()
    }

    // Clean up after exiting the VM context.
    private fun postExecute(context: VMContext) {
      if (logging.isEnabled(LogLevel.TRACE))
        logging.trace("Leaving VM execution context: $threadName")
      context.leave()
    }

    // Lock for execution.
    private fun withLock(op: (VMContext) -> Unit) {
      logging.trace { "Acquiring locked execution context: $threadName" }
      val ctx = workerContext.get() ?: error("Failed to acquire VM context")
      mutex.lock()
      try {
        preExecute(ctx)
        op.invoke(ctx)
      } catch (err: Throwable) {
        lastException.set(err)
      } finally {
        postExecute(ctx)
        mutex.unlock()
      }
    }

    /** @inheritDoc */
    override fun onEvent(event: NativeVMInvocation<I>, sequence: Long, endOfBatch: Boolean) {
      TODO("Not yet implemented")
    }
  }

  // Private logger.
  private val logging: Logger = Logging.of(NativeContextManagerImpl::class)

  // Atomic reference to the globally-active Engine.
  private val engine: AtomicReference<Engine> = AtomicReference(null)

  // Whether the engine has initialized.
  private val initialized: AtomicReference<Boolean> = AtomicReference(false)

  // Context factory function.
  private val contextFactory: AtomicReference<(Engine) -> VMContext.Builder> = AtomicReference(null)

  // Context configuration function.
  private val contextConfigure: AtomicReference<(VMContext.Builder) -> VMContext> = AtomicReference(null)

  // Counter for VM thread spawns.
  private val threadCounter: AtomicInteger = AtomicInteger(0)

  // VM thread factory.
  private val threadFactory: ThreadFactory = ThreadFactory { runnable ->
    val thread = Thread(runnable, "elide-vm-${threadCounter.getAndIncrement()}")
    thread.isDaemon = true
    thread
  }

  // Thread-local VM execution context.
  private val workerContext: VMThreadLocal = VMThreadLocal()

  // Factory for VM invocation events.
  private val invocationEventFactory: VMInvocationFactory<ExecutionInputs> = VMInvocationFactory()

  // Main VM executor.
  private val vmExecutor: NativeVMExecutor<ExecutionInputs> = NativeVMExecutor()

  // Additional properties to apply to created contexts.
  private val additionalProperties: MutableSet<VMProperty> = TreeSet()

  // Disruptor instance for the VM executor.
  private val disruptor: Disruptor<NativeVMInvocation<ExecutionInputs>> = Disruptor(
    invocationEventFactory,
    ringBufferSize,
    threadFactory,
    ProducerType.MULTI,
    SleepingWaitStrategy(sleepingRetries, defaultSleepNs),
  ).apply {
    handleEventsWith(vmExecutor)
  }

  init {
    // Acquire a global engine singleton.
    engine.set(
      Engine.newBuilder(*(config.languages ?: GuestVMConfiguration.DEFAULT_LANGUAGES).toTypedArray()).apply {
        // stub streams
        if (System.getProperty("elide.js.vm.enableStreams", "false") != "true") {
          `in`(StubbedInputStream.SINGLETON)
          out(StubbedOutputStream.SINGLETON)
          err(StubbedOutputStream.SINGLETON)
        }

        // forbid system property overrides
        useSystemProperties(false)

        // allow experimental options
        allowExperimentalOptions(true).let {
          // Apply static engine options.
          staticEngineOptions.fold(it) { builder, property ->
            builder.option(property.symbol, property.value())
          }
        }
      }.build()
    )
  }

  // Allocate a new thread-confined VM execution context.
  private fun allocateContext(builder: ((VMContext.Builder) -> Unit)? = null): VMContext {
    check(initialized.get()) {
      "Cannot allocate VM context: Engine is not initialized"
    }
    val fresh = contextFactory.get().invoke(
      engine()
    )

    // apply properties installed via `configureVM`
    if (additionalProperties.isNotEmpty()) {
      logging.debug("Applying ${additionalProperties.size} additional VM properties")
      additionalProperties.mapNotNull {
        val value = it.value()
        if (value == null) {
          null
        } else {
          it to value
        }
      }.forEach {
        fresh.option(it.first.symbol, it.second)
      }
    } else {
      logging.trace("No additional VM properties to apply")
    }

    // let the call-level builder have a chance to configure things
    builder?.invoke(fresh)

    // finalize the new context and return
    return contextConfigure.get().invoke(fresh)
  }

  /** @inheritDoc */
  override fun configureVM(props: Stream<VMProperty>) {
    check(!initialized.get()) {
      "Cannot configure VM context properties after initialization"
    }
    props.forEach {
      additionalProperties.add(it)
    }
  }

  /** @inheritDoc */
  override fun installContextFactory(factory: (Engine) -> VMContext.Builder) {
    logging.trace("VM installed context factory")
    contextFactory.set(factory)
  }

  /** @inheritDoc */
  override fun installContextSpawn(factory: (VMContext.Builder) -> VMContext) {
    logging.trace("VM installed context spawn")
    contextConfigure.set(factory)
  }

  /** @inheritDoc */
  override fun activate(start: Boolean) {
    logging.trace("Activating native VM context manager")
    initialized.compareAndSet(
      false,
      true,
    )
    if (start) {
      disruptor.start()
    }
  }

  /** @inheritDoc */
  override fun engine(): Engine = engine.get()

  /** @inheritDoc */
  override fun <R> executeAsync(operation: VMContext.() -> R): CompletableFuture<R> {
    // @TODO(sgammon): safe concurrent execution
//    val completed = CountDownLatch(1)
//
//    disruptor.ringBuffer.publishEvent { event, sequence ->
//
//    }

    TODO("not yet implemented")
  }

  /** @inheritDoc */
  override fun <R> acquire(builder: ((VMContext.Builder) -> Unit)?, operation: VMContext.() -> R): R {
    if (!initialized.get()) {
      // @TODO(sgammon): activation of disruptor that isn't based on inherent race condition
      activate(start = true)
    }
    val ctx = allocateContext(builder)
    try {
      ctx.enter()
      return operation.invoke(ctx)
    } finally {
      ctx.leave()
    }
  }
}
