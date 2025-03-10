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
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.SingleTickSegment
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation
import tools.aqua.stars.owa.coverage.metrics.*
import tools.aqua.stars.owa.coverage.metrics.ObservedInstancesMetric

fun main() {
  val maxSegments = 1_000_000
  val sampleSize = 250
  // Probability true/false to probability of being unknown, if true was rolled. The real value in
  // case of unknown is calculated by the first probability again.
  val unknownProbabilities =
      listOf(
          0.5 to 0.0,
          0.5 to 0.0,
          0.5 to 0.1,
          0.5 to 0.1,
          0.5 to 0.1,
          0.5 to 0.1,
          0.1 to 0.1,
          0.1 to 0.1,
          0.5 to 0.5,
          0.5 to 0.5,
      )
  val maxSize = (2.0).pow(unknownProbabilities.size.toDouble()).toInt()

  val tsc = tsc(size = unknownProbabilities.size)
  val segments = generateTicks(probabilities = unknownProbabilities, maxSegments = maxSegments)
  val metric = ObservedInstancesMetric(sampleSize = sampleSize, maxSize = maxSize)

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

@Suppress("SameParameterValue")
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
                        val valuation: Boolean
                        val inverse: Boolean
                        val realValue: Boolean

                        if (Random.nextDouble() < probUncertain) {
                          valuation = false
                          inverse = false
                          realValue = Random.nextDouble() < probTF
                        } else {
                          valuation = Random.nextDouble() < probTF
                          inverse = !valuation
                          realValue = valuation
                        }

                        Valuation(
                            condition = valuation,
                            inverseCondition = inverse,
                            realValue = realValue)
                      }))
          .also { index++ }
    } else null
  }
}
