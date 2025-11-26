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
import tools.aqua.stars.core.evaluation.TickSequence
import tools.aqua.stars.core.hooks.EvaluationHookResult
import tools.aqua.stars.core.hooks.PreTickEvaluationHook
import tools.aqua.stars.core.utils.ApplicationConstantsHolder
import tools.aqua.stars.owa.coverage.dataclasses.IndexTickUnit
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation
import tools.aqua.stars.owa.coverage.metrics.ObservedInstancesMetric

const val EXPERIMENT_SEED = 10101L
val random = Random(EXPERIMENT_SEED)

fun main() {
  ApplicationConstantsHolder.logFolder = "/tmp/data"

  val tagsAndSampleSize =
      listOf(
          1 to 1,
          2 to 1,
          3 to 1,
          4 to 1,
          5 to 1,
          6 to 1,
          7 to 1,
          8 to 10,
          9 to 100,
          10 to 1000,
          15 to 10_000,
          20 to 100_000)
  val probabilities = listOf(.05, .10, .15, .20)

  tagsAndSampleSize.forEach { (numTags, sampleSize) ->
    for (numOpenTags in 1..numTags) {
      probabilities.forEach { probability ->
        runExperimentWithConfig(
            numOpenTags = numOpenTags,
            numClosedTags = numTags - numOpenTags,
            sampleSize = sampleSize,
            probability = probability)
      }
    }
  }
}

/**
 * Runs one experiment configuration.
 *
 * @param numOpenTags Number of open tags.
 * @param numClosedTags Number of closed tags.
 * @param sampleSize Sample size for the observed instances metric before an evaluation is done.
 * @param probability Probability of generating an unknown observation for open tags.
 */
private fun runExperimentWithConfig(
    numOpenTags: Int,
    numClosedTags: Int,
    sampleSize: Int,
    probability: Double
) {
  val numTags = numOpenTags + numClosedTags
  val identifier = "no=${numOpenTags}_nc=${numClosedTags}_p=${probability}"

  println(
      "Running random experiment with configuration: numOpenTags=$numOpenTags, numClosedTags=$numClosedTags, probability:$probability, sampleSize=$sampleSize")

  val maxSize = (2.0).pow(numTags).toInt()

  val tsc = randomTSC(size = numTags)
  val ticks =
      generateTicks(
          numOpenTags = numOpenTags, numClosedTags = numClosedTags, probability = probability)
  val metric =
      ObservedInstancesMetric(sampleSize = sampleSize, maxSize = maxSize, identifier = identifier)

  TSCEvaluation(
          tscList = listOf(tsc),
          writePlots = true,
          writePlotDataCSV = true,
          loggerIdentifier = identifier)
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
}

@Suppress("SameParameterValue")
private fun generateTicks(
    numOpenTags: Int,
    numClosedTags: Int,
    probability: Double,
): TickSequence<UnknownTickData> {
  var index = 0
  return TickSequence {
    UnknownTickData(
            currentTick = IndexTickUnit(index),
            unknownData =
                generateTags(numOpenTags, true, probability) +
                    generateTags(numClosedTags, false, probability))
        .also {
          @Suppress("AssignedValueIsNeverRead") // Variable is used in the generator
          index++
        }
  }
}

/**
 * Generates #[numTags] tags. If [isOpen] is `true`, then the tag is unknown with a probability of
 * [probability]. The value `true`/`false` is generated 50/50.
 *
 * @param numTags The number of tags to generate.
 * @param isOpen Whether to generate unknown observations with a probability of [probability].
 */
private fun generateTags(numTags: Int, isOpen: Boolean, probability: Double): List<Valuation> =
    (0 until numTags).map {
      val valuation: Boolean
      val inverse: Boolean
      val realValue: Boolean

      if (isOpen && random.nextDouble() < probability) {
        // Generate unknown
        valuation = false
        inverse = false
        realValue = random.nextDouble() < .5
      } else {
        // Generate known
        valuation = random.nextDouble() < .5
        inverse = !valuation
        realValue = valuation
      }

      Valuation(condition = valuation, inverseCondition = inverse, realValue = realValue)
    }
