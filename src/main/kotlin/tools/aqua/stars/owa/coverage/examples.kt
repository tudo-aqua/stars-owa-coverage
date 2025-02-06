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

package tools.aqua.stars.owa.coverage

import tools.aqua.stars.core.tsc.builder.tsc
import tools.aqua.stars.data.av.dataclasses.Actor
import tools.aqua.stars.data.av.dataclasses.Segment
import tools.aqua.stars.data.av.dataclasses.TickData
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds

fun tscExample() =
    tsc<Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds>() {
      all("TSC") {
        optional("Dynamic Relation") {
          leaf("Must Yield")
          leaf("Pedestrian Crossed")
        }
        exclusive("Street Type") {
          leaf("Junction")
          leaf("Road")
        }
        bounded("Stop Type", 0 to 1) {
          leaf("Stop Sign")
          leaf("Yield Sign")
          leaf("Red Light")
        }
      }
    }

fun main() {
  print(tscExample().possibleTSCInstances.size)
}
