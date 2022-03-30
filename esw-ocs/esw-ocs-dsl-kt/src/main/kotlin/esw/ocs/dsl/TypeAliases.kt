/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl

import kotlinx.coroutines.CoroutineScope

typealias SuspendableCallback = suspend CoroutineScope.() -> Unit
typealias SuspendableConsumer<T> = suspend CoroutineScope.(T) -> Unit
typealias SuspendableSupplier<T> = suspend CoroutineScope.() -> T