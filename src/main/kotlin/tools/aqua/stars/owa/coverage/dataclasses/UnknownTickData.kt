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

import tools.aqua.stars.core.types.TickDataType
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds

class UnknownTickData(
    override val currentTick: TickDataUnitSeconds,
    val unknownData: List<Valuation>
) :
    TickDataType<
        NoEntity,
        UnknownTickData,
        SingleTickSegment,
        TickDataUnitSeconds,
        TickDataDifferenceSeconds> {
  override var entities: List<NoEntity> = emptyList()
  override lateinit var segment: SingleTickSegment

  override fun toString(): String =
      unknownData.joinToString(
          prefix = "${currentTick.tickSeconds.toInt()}: [", separator = ", ", postfix = "]") {
            it.toString()
          }
}
