package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.location.models.AkkaLocation
import csw.location.models.ComponentId
import csw.location.models.ComponentType
import csw.location.models.Connection
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.future.await
import scala.concurrent.duration.Duration.create

interface CommandServiceDsl {
    val locationService: ILocationService
    val actorSystem: ActorSystem<*>

    private val duration: Duration
        get() = Duration.ofSeconds(10)

    private val timeout: Timeout
        get() = Timeout(create(10, TimeUnit.SECONDS))

    fun setup(prefix: String, commandName: String, obsId: String?) =
        Setup(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun observe(prefix: String, commandName: String, obsId: String?) =
        Observe(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    /************* Assembly *************/
    suspend fun validateAssemblyCommand(assemblyName: String, command: ControlCommand): ValidateResponse =
        validate(assemblyName, Assembly, command)

    suspend fun submitCommandToAssembly(assemblyName: String, command: ControlCommand): SubmitResponse =
        submit(assemblyName, Assembly, command)

    suspend fun submitAndWaitCommandToAssembly(assemblyName: String, command: ControlCommand): SubmitResponse =
        submitAndWait(assemblyName, Assembly, command)

    suspend fun oneWayCommandToAssembly(assemblyName: String, command: ControlCommand): OnewayResponse =
        oneWay(assemblyName, Assembly, command)

    /************* HCD *************/
    suspend fun validateHcdCommand(hcdName: String, command: ControlCommand): ValidateResponse =
        validate(hcdName, HCD, command)

    suspend fun submitCommandToHcd(hcdName: String, command: ControlCommand): SubmitResponse =
        submit(hcdName, HCD, command)

    suspend fun submitAndWaitCommandToHcd(hcdName: String, command: ControlCommand): SubmitResponse =
        submitAndWait(hcdName, HCD, command)

    suspend fun oneWayCommandToHcd(hcdName: String, command: ControlCommand): OnewayResponse =
        oneWay(hcdName, HCD, command)

    /******************************/
    private suspend fun validate(name: String, compType: ComponentType, command: ControlCommand): ValidateResponse =
        send(name, compType) { it.validate(command) }

    private suspend fun submit(name: String, compType: ComponentType, command: ControlCommand): SubmitResponse =
        send(name, compType) { it.submit(command, timeout) }

    private suspend fun submitAndWait(name: String, compType: ComponentType, command: ControlCommand): SubmitResponse =
        send(name, compType) { it.submitAndWait(command, timeout) }

    private suspend fun oneWay(name: String, compType: ComponentType, command: ControlCommand): OnewayResponse =
        send(name, compType) { it.oneway(command, timeout) }

    private suspend fun <T> send(
        name: String,
        compType: ComponentType,
        action: (commandService: ICommandService) -> CompletableFuture<T>
    ) = resolve(name, compType)
        .orElseThrow { IllegalArgumentException("Could not find component - $name of type - $compType") }
        .let { action(CommandServiceFactory.jMake(it, actorSystem)).await() }

    private suspend fun resolve(name: String, compType: ComponentType): Optional<AkkaLocation> =
        locationService.resolve(Connection.AkkaConnection(ComponentId(name, compType)), duration).await()

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}
