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
import tools.aqua.stars.data.av.dataclasses.Actor
import tools.aqua.stars.data.av.dataclasses.TickData
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.logic.kcmftbl.Interval.Companion.rangeTo
import tools.aqua.stars.logic.kcmftbl.firstorder.exists
import tools.aqua.stars.logic.kcmftbl.past.historically
import tools.aqua.stars.logic.kcmftbl.past.once
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import kotlin.math.sign

fun randomTSC(size: Int): TSC<NoEntity, UnknownTickData, TickDataUnitSeconds, TickDataDifferenceSeconds> =
    tsc {
      all("TSCRoot") {
        repeat(size) {
          addEdge(
              TSCEdge(
                  condition = { td -> td.unknownData[it].condition },
                  inverseCondition = { td -> td.unknownData[it].inverseCondition },
                  destination = TSCLeafNode("Leaf $it", emptyMap())))
        }
      }
    }

fun simTSC(): TSC<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds> =
  tsc {
    all("TSCRoot") {
      leaf("t3") {
        condition {
          exists(it.vehicles) { v ->
            it.ego.boundingBox.toBoundingBox2D().extendFront(minDistanceToFront((it.ego.velocity.magnitude()))).collidesWith(v.boundingBox.toBoundingBox2D())
          }
        }
        inverseCondition {
          !exists(it.vehicles) { v ->
            it.ego.boundingBox.toBoundingBox2D().extendFront(it.ego.velocity.magnitude() * 3.6 / 2).collidesWith(v.boundingBox.toBoundingBox2D()) // Velocity in km/h divided in half ("half tacho value"))
          }
        }
      }

      leaf("t6") {
        condition {
          exists(it.vehicles) { v ->
            v.lane.road == it.ego.lane.road && v.lane.laneId.sign == it.ego.lane.laneId.sign
          }
        }
      }

      leaf("t7") {
        condition {
          // Check that at least 10 seconds have passed
          once(it, TickDataDifferenceSeconds(10.0) .. TickDataDifferenceSeconds(Double.POSITIVE_INFINITY)) { true } &&

              // Check that there is at least one vehicle in ego lane within 30m in front of ego
              exists(it.vehicles) { v ->
                historically(it, TickDataDifferenceSeconds(10.0) .. TickDataDifferenceSeconds(10.0)) { t ->
                  val v0 = t.vehicles.firstOrNull { v1 -> v1.id == v.id} ?: return@historically false

                  v0.lane == t.ego.lane && v0.boundingBox.toBoundingBox2D().collidesWith(it.ego.boundingBox.toBoundingBox2D().extendFront(30.0))
                }
              }
        }
      }

      leaf("t8") {
        condition { it.ego.velocity.magnitude() > it.ego.lane.speedAt(it.ego.positionOnLane)}
      }
    }
  }

/*
 * UN-R 157 – 5.2.3.3
 * The activated system shall detect the distance to the next vehicle in front as defined in
 * paragraph 7.1.1. and shall adapt the vehicle speed in order to avoid collision. While the ALKS
 * vehicle is not at standstill, the system shall adapt the speed to adjust the distance to a
 * vehicle in front in the same lane to be equal or greater than the minimum following distance. In
 * case the minimum time gap cannot be respected temporarily because of other road users (e.g.
 * vehicle is cutting in, decelerating lead vehicle, etc.), the vehicle shall readjust the minimum
 * following distance at the next available opportunity without any harsh braking unless an
 * emergency manoeuvre would become necessary.
 *
 * The minimum following distance shall be calculated using the formula:
 * d_min = v_ALKS* t_front
 *
 * Where:
 * d_min = the minimum following distance
 * v_ALKS = the present speed of the ALKS vehicle in m/s
 * t_front = minimum time gap in seconds between the ALKS vehicle and a leading vehicle in front as per the table below:
 *
 *   Present speed of       Minimum        Minimum
 *   the ALKS vehicle       time gap  following distance
 * (km/h)       (m/s)          (s)          (m)
 *   7.2         2.0           1.0          2.0
 *   10          2.78          1.1          3.1
 *   20          5.56          1.2          6.7
 *   30          8.33          1.3          10.8
 *   40          11.11         1.4          15.6
 *   50          13.89         1.5          20.8
 *   60          16.67         1.6          26.7
 *
 * For speed values not mentioned in the table, linear interpolation shall be applied.
 * Notwithstanding the result of the formula above for present speeds below 2 m/s
 * the minimum following distance shall never be less than 2 m.
 *
 */

/**
 * Minimal distance to the vehicle in front.
 *
 * @param velocity the velocity of the vehicle in m/s.
 */
//TODO: "Implementation is off. Interpolation between points must be applied"
@Suppress("MagicNumber")
fun minDistanceToFront(velocity: Double): Double =
  when (velocity) {
    in 0.0 ..< 2.0 -> 2.0
    in 2.0 ..< 7.2 -> velocity * 1.0
    in 7.2 ..< 10.0 -> velocity * 1.1
    in 10.0 ..< 20.0 -> velocity * 1.2
    in 20.0 ..< 30.0 -> velocity * 1.3
    in 30.0 ..< 40.0 -> velocity * 1.4
    in 40.0 ..< 50.0 -> velocity * 1.5
    in 50.0 ..< 60.0 -> velocity * 1.6
    else -> velocity * 3.6 / 2 // Velocity in km/h divided in half ("half tacho value")
  }