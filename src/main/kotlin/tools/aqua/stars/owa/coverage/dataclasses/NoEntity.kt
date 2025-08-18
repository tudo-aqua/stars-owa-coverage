/*
 * Copyright 2025 The STARS OWA Coverage Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.stars.owa.coverage.dataclasses

import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds

class NoEntity(val tickData: UnknownTickData) :
    EntityType<NoEntity, UnknownTickData, TickDataUnitSeconds, TickDataDifferenceSeconds>() {

  val id: Int = -1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is NoEntity) return false
    return id == other.id && tickData == other.tickData
  }

  override fun hashCode(): Int = id * 31 + tickData.hashCode()
}
