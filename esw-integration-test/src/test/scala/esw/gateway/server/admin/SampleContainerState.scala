package esw.gateway.server.admin

import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType.ChoiceKey
import csw.params.core.models.{Choice, Choices}
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.{Prefix, Subsystem}

object SampleContainerState {

  val shutdownChoice: Choice = Choice("Shutdown")
  val initChoice: Choice     = Choice("Initialize")

  val choices: Choices      = Choices.fromChoices(initChoice, shutdownChoice)
  val choiceKey: GChoiceKey = ChoiceKey.make("choiceKey", choices)

  //filter assembly
  val eswAssemblyPrefix: Prefix      = Prefix("ESW.filter")
  val eswAssemblyCompId: ComponentId = ComponentId(eswAssemblyPrefix, ComponentType.Assembly)

  //Galil HCD
  val eswGalilHcdPrefix: Prefix      = Prefix("ESW.Galil")
  val eswGalilHcdCompId: ComponentId = ComponentId(eswGalilHcdPrefix, ComponentType.HCD)

  //esw container
  val eswContainerPrefix: Prefix      = Prefix(Subsystem.Container, "ESW_Container")
  val eswContainerCompId: ComponentId = ComponentId(eswContainerPrefix, ComponentType.Container)

  //current state which gets published from shutdown handler of galil HCD
  val galilShutdownCurrentState: CurrentState =
    CurrentState(eswGalilHcdPrefix, StateName("Shutdown_Galil"), Set(choiceKey.set(shutdownChoice)))

  //current state which gets published from initialize handler of galil HCD
  val galilInitializeCurrentState: CurrentState =
    CurrentState(eswGalilHcdPrefix, StateName("Initializing_Galil"), Set(choiceKey.set(initChoice)))

  //current state which gets published from shutdown handler of filter Assembly
  val assemblyShutdownCurrentState: CurrentState =
    CurrentState(eswAssemblyPrefix, StateName("Shutdown_Filter_Assembly"), Set(choiceKey.set(shutdownChoice)))

  //current state which gets published from initialize handler of filter Assembly
  val assemblyInitializeCurrentState: CurrentState =
    CurrentState(eswAssemblyPrefix, StateName("Initializing_Filter_Assembly"), Set(choiceKey.set(initChoice)))

}
