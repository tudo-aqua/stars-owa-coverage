/*
 * Copyright 2023-2025 The STARS OWA Coverage Authors
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


import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.tsc.builder.*
import tools.aqua.stars.core.tsc.edge.TSCEdge
import tools.aqua.stars.core.tsc.node.TSCLeafNode
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.SingleTickSegment
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.metrics.ObservedInstancesMetric

fun tsc(size: Int) =
  tsc<NoEntity, UnknownTickData, SingleTickSegment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
    all("TSCRoot") {
      repeat(size) {
        addEdge(
          TSCEdge(
            condition = { td -> td.segment.tick.unknownData[it].first },
            inverseCondition = { td -> td.segment.tick.unknownData[it].second },
            destination = TSCLeafNode<NoEntity, UnknownTickData, SingleTickSegment, TickDataUnitSeconds, TickDataDifferenceSeconds>("Leaf $it", emptyMap(), emptyMap()) {}
          )
        )
      }
    }
  }


fun main() {
  val numSegments = 10
  // Probability true/false to probability of being unknown, if true was rolled
  val unknownProbabilities = listOf(
    0.5 to 0.0,
    0.5 to 0.0,
//    0.5 to 0.1,
//    0.5 to 0.1,
//    0.5 to 0.5
  )

  val tsc = tsc(size = unknownProbabilities.size)
  println("TSC:\n$tsc\n")

  val ticks = generateTicks(probabilities = unknownProbabilities, numSegments = numSegments)
  println(ticks.joinToString("\n") { it.toString() })

  val segments = ticks.map { SingleTickSegment(it) }.asSequence()

  val metric = ObservedInstancesMetric()
  TSCEvaluation(tscList = listOf(tsc)).apply {
    registerMetricProviders(metric)
  }.runEvaluation(segments = segments)

  println(metric.observedCertainInstanceCount.values.first())
}

private fun generateTicks(probabilities: List<Pair<Double, Double>>, numSegments: Int) : List<UnknownTickData> =
  (0 until numSegments).map { index ->
    UnknownTickData(
      currentTick = TickDataUnitSeconds(index.toDouble()),
      unknownData = probabilities.map { (probTF, probUncertain) ->
        val valuation = Math.random() < probTF
        val inverse = if (Math.random() < probUncertain) false else !valuation // Opposite of valuation with probability of being unknown (false, false)
        valuation to inverse
      }
    )
  }