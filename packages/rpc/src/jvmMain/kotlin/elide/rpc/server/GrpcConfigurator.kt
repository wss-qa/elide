package elide.rpc.server

import io.grpc.ServerBuilder
import io.grpc.health.v1.HealthGrpc
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import jakarta.inject.Inject

/**
 * Bean creation listener which is responsible for observing the creation of a gRPC server, as applicable; configuration
 * is intercepted to facilitate additional features (compression, security, health monitoring), and service state is
 * additionally relayed to [RpcRuntime] for registered service resolution.
 *
 * @param runtime RPC runtime which should be notified of service configuration state.
 * @param healthManager Manager which keeps track of individual service state/health.
 */
@Context
internal class GrpcConfigurator @Inject constructor (
  private val runtime: RpcRuntime,
  private val healthManager: ServiceHealthManager
): BeanCreatedEventListener<ServerBuilder<*>> {
  init {
    // start health status monitor in boot mode, by indicating `NOT_SERVING` for the health service itself.
    healthManager.notifyPending(HealthGrpc.getServiceDescriptor())
  }

  /** @inheritDoc */
  override fun onCreated(event: BeanCreatedEvent<ServerBuilder<*>>): ServerBuilder<*> {
    // grab the server as it is being created
    val builder = runtime.configureServer(
      event.bean
    )

    // notify the runtime of available services
    // @TODO(sgammon): avoid double-building the gRPC server
    val server = builder.build()
    runtime.registerServices(server.services)

    healthManager.notifyServing(
      HealthGrpc.getServiceDescriptor()
    )
    runtime.notifyReady()

    // mount reflection, if so directed
    return builder
  }
}
