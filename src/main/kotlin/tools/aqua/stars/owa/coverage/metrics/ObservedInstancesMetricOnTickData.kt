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

package tools.aqua.stars.owa.coverage.metrics

import tools.aqua.stars.core.metrics.providers.Plottable
import tools.aqua.stars.core.metrics.providers.TSCInstanceMetricProvider
import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.tsc.instance.TSCInstance
import tools.aqua.stars.data.av.dataclasses.Actor
import tools.aqua.stars.data.av.dataclasses.TickData
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.Valuation

/** @param sampleSize Number of segments to evaluate before updating the metric. */
@Suppress("DuplicatedCode")
class ObservedInstancesMetricOnTickData(
    tsc: TSC<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds>,
    sampleSize: Int = 1,
    maxSize: Int = tsc.instanceCount.toInt()
) :
    AbstractObservedInstancesMetric<Actor, TickData>(sampleSize = sampleSize, maxSize = maxSize),
    TSCInstanceMetricProvider<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds>,
    Plottable {

  val tags = tsc.toList().map { it.label }
  var tickCount: Int = 0
  val unknownOccurrences: MutableMap<String, Int> = tags.associateWith { 0 }.toMutableMap()

  override fun evaluate(
      tscInstance: TSCInstance<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds>
  ) {
    val bitmask = MutableList(tags.size) { Valuation.FALSE }

    tscInstance.forEach {
      val index = tags.indexOf(it.label)

      if (it.isUnknown) {
        unknownOccurrences[it.label] = unknownOccurrences.getValue(it.label)
        bitmask[index] = Valuation.UNKNOWN
      } else {
        bitmask[index] = Valuation.TRUE
      }
    }

    tickCount++

    super.evaluate(bitmask)
  }

  fun printSummary() {
    println("Tick count: $tickCount")
    println("Unknown occurrences:")
    unknownOccurrences.forEach { (tag, count) ->
      println("  $tag: $count (${count.toDouble() / tickCount * 100}%)")
    }
  }
}
