/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

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

  // filter assembly
  val eswAssemblyPrefix: Prefix      = Prefix("ESW.filter")
  val eswAssemblyCompId: ComponentId = ComponentId(eswAssemblyPrefix, ComponentType.Assembly)

  // Galil HCD
  val eswGalilHcdPrefix: Prefix      = Prefix("ESW.Galil")
  val eswGalilHcdCompId: ComponentId = ComponentId(eswGalilHcdPrefix, ComponentType.HCD)

  // esw container
  val eswContainerPrefix: Prefix      = Prefix(Subsystem.Container, "ESW_Container")
  val eswContainerCompId: ComponentId = ComponentId(eswContainerPrefix, ComponentType.Container)

  val shutdownGalilStateName: StateName = StateName("Shutdown_Galil")
  // current state which gets published from shutdown handler of galil HCD
  val galilShutdownCurrentState: CurrentState =
    CurrentState(eswGalilHcdPrefix, shutdownGalilStateName, Set(choiceKey.set(shutdownChoice)))

  val initializingGalilStateName: StateName = StateName("Initializing_Galil")
  // current state which gets published from initialize handler of galil HCD
  val galilInitializeCurrentState: CurrentState =
    CurrentState(eswGalilHcdPrefix, initializingGalilStateName, Set(choiceKey.set(initChoice)))

  val shutdownFilterAssemblyStateName: StateName = StateName("Shutdown_Filter_Assembly")
  // current state which gets published from shutdown handler of filter Assembly
  val assemblyShutdownCurrentState: CurrentState =
    CurrentState(eswAssemblyPrefix, shutdownFilterAssemblyStateName, Set(choiceKey.set(shutdownChoice)))

  val initializingFilterAssemblyStateName: StateName = StateName("Initializing_Filter_Assembly")
  // current state which gets published from initialize handler of filter Assembly
  val assemblyInitializeCurrentState: CurrentState =
    CurrentState(eswAssemblyPrefix, initializingFilterAssemblyStateName, Set(choiceKey.set(initChoice)))

}
