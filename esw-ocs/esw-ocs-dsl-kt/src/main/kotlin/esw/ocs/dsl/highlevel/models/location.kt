/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("unused")

package esw.ocs.dsl.highlevel.models

import csw.location.api.javadsl.JComponentType
import csw.location.api.javadsl.JConnectionType
import csw.location.api.models.*
import csw.prefix.models.Prefix

/* ******** Helpers to create ComponentType ******** */
val Container: ComponentType = JComponentType.Container
val HCD: ComponentType = JComponentType.HCD
val Assembly: ComponentType = JComponentType.Assembly
val Sequencer: ComponentType = JComponentType.Sequencer
val SequenceComponent: ComponentType = JComponentType.SequenceComponent
val Service: ComponentType = JComponentType.Service

/* ******** Helpers to create ConnectionType ******** */
val AkkaType: ConnectionType = JConnectionType.AkkaType
val TcpType: ConnectionType = JConnectionType.TcpType
val HttpType: ConnectionType = JConnectionType.HttpType

/* ******** Helpers to access values from models ******** */
val Connection.connectionType: ConnectionType get() = connectionType()
val Connection.componentId: ComponentId get() = componentId()

val ComponentId.componentType: ComponentType get() = componentType()

val Location.connection: Connection get() = connection()
val Location.prefix: Prefix get() = prefix()
val Location.prefixStr: String get() = prefix.toString()

// ============================================
fun Prefix(value: String): Prefix = Prefix.apply(value)

data class RegistrationResult(val location: Location, val unregister: () -> Unit)
