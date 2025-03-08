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

package tools.aqua.stars.owa.coverage

import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.tsc.builder.tsc
import tools.aqua.stars.core.tsc.edge.TSCEdge
import tools.aqua.stars.core.tsc.node.TSCLeafNode
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.SingleTickSegment
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData

fun tsc(
    size: Int
): TSC<
    NoEntity, UnknownTickData, SingleTickSegment, TickDataUnitSeconds, TickDataDifferenceSeconds> =
    tsc<
        NoEntity,
        UnknownTickData,
        SingleTickSegment,
        TickDataUnitSeconds,
        TickDataDifferenceSeconds> {
      all("TSCRoot") {
        repeat(size) {
          addEdge(
              TSCEdge(
                  condition = { td -> td.segment.tick.unknownData[it].condition },
                  inverseCondition = { td -> td.segment.tick.unknownData[it].inverseCondition },
                  destination =
                      TSCLeafNode<
                          NoEntity,
                          UnknownTickData,
                          SingleTickSegment,
                          TickDataUnitSeconds,
                          TickDataDifferenceSeconds>(
                          "Leaf $it", emptyMap(), emptyMap()) {}))
        }
      }
    }
