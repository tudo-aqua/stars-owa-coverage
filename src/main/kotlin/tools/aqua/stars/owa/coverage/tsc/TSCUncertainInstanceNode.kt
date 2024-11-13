/*
 * Copyright 2024 The STARS OWA Coverage Authors
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

package tools.aqua.stars.owa.coverage.tsc

import tools.aqua.stars.core.tsc.instance.TSCInstanceNode
import tools.aqua.stars.core.tsc.node.TSCNode
import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.core.types.TickDataType
import tools.aqua.stars.core.types.TickDifference
import tools.aqua.stars.core.types.TickUnit

class TSCUncertainInstanceNode<
    E : EntityType<E, T, S, U, D>,
    T : TickDataType<E, T, S, U, D>,
    S : SegmentType<E, T, S, U, D>,
    U : TickUnit<U, D>,
    D : TickDifference<D>>(
    tscNode: TSCNode<E, T, S, U, D>,
) : TSCInstanceNode<E, T, S, U, D>(tscNode) {
  override fun toString(depth: Int): String =
      StringBuilder()
          .apply {
            append(if (value is Unit) "\n" else "($value)\n")
            append("  ".repeat(depth))
            append("--> Uncertain($label)")
            edges.forEach { instanceEdge -> append(instanceEdge.destination.toString(depth + 1)) }
          }
          .toString()
}
