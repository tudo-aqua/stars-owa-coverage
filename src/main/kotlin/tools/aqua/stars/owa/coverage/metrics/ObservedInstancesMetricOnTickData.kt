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
import tools.aqua.stars.core.tsc.instance.TSCInstanceNode
import tools.aqua.stars.data.av.dataclasses.Actor
import tools.aqua.stars.data.av.dataclasses.TickData
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.Valuation

/** @param sampleSize Number of segments to evaluate before updating the metric. */
@Suppress("DuplicatedCode")
class ObservedInstancesMetricOnTickData(tsc: TSC<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds>, sampleSize: Int = 1, maxSize: Int = tsc.instanceCount.toInt()) :
    AbstractObservedInstancesMetric<Actor, TickData>(sampleSize = sampleSize, maxSize = maxSize),
    TSCInstanceMetricProvider<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds>,
    Plottable {

  val tags: List<String> = tsc.toList().map { it.label }

  override fun evaluate(tscInstance: TSCInstance<Actor, TickData, TickDataUnitSeconds, TickDataDifferenceSeconds>) {
    super.evaluate(tscInstance.toList().toBitmask())
  }

  private fun List<TSCInstanceNode<*,*,*,*>>.toBitmask(): Bitmask =
    List(tags.size) { i ->
      val node = this.firstOrNull { it.label == tags[i] }

      when {
        // Node not in instance -> known false
        node == null -> Valuation.FALSE

        // Node is unknown
        node.isUnknown -> Valuation.UNKNOWN

        // Node is known true
        else -> Valuation.TRUE
      }
    }

}
