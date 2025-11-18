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

import org.jetbrains.letsPlot.commons.intern.math.ipow
import kotlin.random.Random
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.evaluation.TickSequence
import tools.aqua.stars.core.hooks.EvaluationHookResult
import tools.aqua.stars.core.hooks.PreTickEvaluationHook
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation
import tools.aqua.stars.owa.coverage.metrics.ObservedInstancesMetricOnKnownData

const val EXPERIMENT_SEED = 10101L

fun runMatrixExperiment() {
  val tagsAndSampleSize = listOf(
    1 to 1,
    2 to 1,
    3 to 1,
    4 to 1,
    5 to 1,
    6 to 1,
    7 to 1,
    8 to 10,
    9 to 10,
    10 to 100,
    15 to 10_000,
    20 to 1_000_000)
  val probabilities = listOf(.10, .15, .20)

  tagsAndSampleSize.forEach { (numTags, sampleSize) ->
    probabilities.forEach { probability ->
      runRandomExperimentWithConfig(numTags = numTags, sampleSize = sampleSize, probability = probability)
    }
  }
}

fun runRandomExperiment(args: Array<String>) {
  val numTags = args.getOrNull(0)?.toIntOrNull() ?: throw IllegalArgumentException(ARGS_ERROR)
  val maxTicks = args.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException(ARGS_ERROR)
  val probability = args.getOrNull(2)?.toIntOrNull() ?: throw IllegalArgumentException(ARGS_ERROR)
  val seed = args.getOrNull(3)?.toLongOrNull() ?: EXPERIMENT_SEED

  runRandomExperimentWithConfig(
      numTags = numTags,
      sampleSize = maxTicks /1000,
      probability = probability / 100.0,
      seed = seed,
      maxTicks = maxTicks)
}

private fun runRandomExperimentWithConfig(
    numTags: Int,
    sampleSize: Int,
    probability: Double,
    seed: Long = EXPERIMENT_SEED,
    maxTicks: Int = -1,
) {
  println(
      "Running random experiment with configuration: numTags=$numTags, probability:$probability, maxTicks=$maxTicks, sampleSize=$sampleSize")


  val maxSize = 2.ipow(numTags).toInt()

  val tsc = randomTSC(size = numTags)
  val ticks = generateTicks(numTags = numTags, probability = probability, maxTicks = maxTicks, seed = seed)
  val metric = ObservedInstancesMetricOnKnownData(sampleSize = sampleSize, maxSize = maxSize)

  TSCEvaluation(tscList = listOf(tsc))
      .apply {
        clearHooks()
        registerMetricProviders(metric)
        registerPreTickEvaluationHooks(
            PreTickEvaluationHook("AbortHook") {
              if ((metric.minUncoverCount.lastOrNull() ?: 0) < maxSize) EvaluationHookResult.OK
              else EvaluationHookResult.CANCEL
            })
      }
      .runEvaluation(ticks = sequenceOf(ticks))
  println("Remaining gap: ${metric.gap}")
}

@Suppress("SameParameterValue")
private fun generateTicks(
    numTags: Int,
    probability: Double,
    maxTicks: Int,
    seed: Long,
): TickSequence<UnknownTickData> {
  val random = Random(seed)
  var index = 0
  return TickSequence {
    if (index < maxTicks || maxTicks == -1) {
      UnknownTickData(
              currentTick = TickDataUnitSeconds(index.toDouble()),
              unknownData =
                (0..numTags).map {
                    val valuation: Boolean
                    val inverse: Boolean
                    val realValue: Boolean

                    if (random.nextDouble() < probability) {
                      valuation = false
                      inverse = false
                      realValue = random.nextDouble() < .5
                    } else {
                      valuation = random.nextDouble() < .5
                      inverse = !valuation
                      realValue = valuation
                    }

                    Valuation(
                        condition = valuation, inverseCondition = inverse, realValue = realValue)
                  })
          .also {
            @Suppress("AssignedValueIsNeverRead") // Variable is used in the generator
            index++
          }
    } else null
  }
}
