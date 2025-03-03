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

import kotlin.math.pow
import kotlin.random.Random
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.hooks.EvaluationHookResult
import tools.aqua.stars.core.hooks.PreSegmentEvaluationHook
import tools.aqua.stars.core.tsc.builder.*
import tools.aqua.stars.core.tsc.edge.TSCEdge
import tools.aqua.stars.core.tsc.node.TSCLeafNode
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.SingleTickSegment
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation
import tools.aqua.stars.owa.coverage.metrics.*
import tools.aqua.stars.owa.coverage.metrics.ObservedInstancesMetric

fun tsc(size: Int) =
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

private fun generateTicks(
    probabilities: List<Pair<Double, Double>>,
    maxSegments: Int
): Sequence<SingleTickSegment> {
  var index = 0
  return generateSequence {
    if (index < maxSegments) {
      SingleTickSegment(
              UnknownTickData(
                  currentTick = TickDataUnitSeconds(index.toDouble()),
                  unknownData =
                      probabilities.map { (probTF, probUncertain) ->
                        val valuation = Random.nextDouble() < probTF
                        val inverse =
                            if (Random.nextDouble() < probUncertain) false
                            else !valuation // Opposite of valuation with probability of being
                        // unknown (false, false)
                        val realValue =
                            if (!valuation && !inverse) Random.nextDouble() < 0.5
                            else valuation // If value is unknown, choose randomly between true and
                        // false
                        Valuation(
                            condition = valuation,
                            inverseCondition = inverse,
                            realValue = realValue)
                      }))
          .also { index++ }
    } else null
  }
}

fun main() {
  val maxSegments = 1_000_000
  val sampleSize = 200
  // Probability true/false to probability of being unknown, if true was rolled
  val unknownProbabilities =
      listOf(
          0.5 to 0.0,
          0.5 to 0.0,
          0.5 to 0.1,
          0.5 to 0.1,
          0.5 to 0.5,
          0.5 to 0.5,
          0.5 to 0.5,
          0.5 to 0.5,
          0.5 to 0.5,
          0.5 to 0.5,
      )
  val maxSize = (2.0).pow(unknownProbabilities.size.toDouble()).toInt()

  val tsc = tsc(size = unknownProbabilities.size)
  println("TSC:\n$tsc\n")

  val segments = generateTicks(probabilities = unknownProbabilities, maxSegments = maxSegments)
  //  println(ticks.joinToString("\n") { it.toString() })
  //
  //  val segments = ticks.map { SingleTickSegment(it) }.asSequence()

  val metric = ObservedInstancesMetric(sampleSize = sampleSize)
  TSCEvaluation(tscList = listOf(tsc))
      .apply {
        registerMetricProviders(metric)
        registerPreSegmentEvaluationHooks(
            PreSegmentEvaluationHook<E, T, S, U, D>("AbortHook") {
              if ((metric.minUncoverCount.lastOrNull() ?: 0) < maxSize) EvaluationHookResult.OK
              else EvaluationHookResult.CANCEL
            })
      }
      .runEvaluation(segments = segments)
  println(metric.certainInstanceCount)
}
