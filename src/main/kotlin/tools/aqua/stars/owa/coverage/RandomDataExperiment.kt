package tools.aqua.stars.owa.coverage

import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.evaluation.TickSequence
import tools.aqua.stars.core.hooks.EvaluationHookResult
import tools.aqua.stars.core.hooks.PreTickEvaluationHook
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation
import tools.aqua.stars.owa.coverage.metrics.ObservedInstancesMetricOnKnownData
import kotlin.math.pow
import kotlin.random.Random

fun runRandomExperiment(args: Array<String>) {
  val numTags = args.getOrNull(0)?.toIntOrNull() ?: throw IllegalArgumentException(ARGS_ERROR)
  val maxTicks = args.getOrNull(1)?.toIntOrNull() ?: throw IllegalArgumentException(ARGS_ERROR)
  val seed = args.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
  val sampleSize = maxTicks / 1000

  println("Running random experiment with configuration: numTags=$numTags, maxTicks=$maxTicks, sampleSize=$sampleSize")

  // Probability true/false to probability of being unknown, if true was rolled. The real value in
  // case of unknown is calculated by the first probability again.
  val unknownProbabilities = mutableListOf<Pair<Double, Double>>()

  repeat(numTags) {
    val probTF = Random.nextDouble(0.0, 1.0)
    val probUncertain = if(Random.nextBoolean()) 0.0 else Random.nextDouble(0.0, 0.5)
    unknownProbabilities.add(probTF to probUncertain)
  }

  val maxSize = (2.0).pow(unknownProbabilities.size.toDouble()).toInt()

  val tsc = randomTSC(size = unknownProbabilities.size)
  val ticks = generateTicks(probabilities = unknownProbabilities, maxTicks = maxTicks, seed = seed)
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
  probabilities: List<Pair<Double, Double>>,
  maxTicks: Int,
  seed: Long,
): TickSequence<UnknownTickData> {
  val random = Random(seed)
  var index = 0
  return TickSequence {
    if (index < maxTicks) {
      UnknownTickData(
        currentTick = TickDataUnitSeconds(index.toDouble()),
        unknownData =
          probabilities.map { (probTF, probUncertain) ->
            val valuation: Boolean
            val inverse: Boolean
            val realValue: Boolean

            if (random.nextDouble() < probUncertain) {
              valuation = false
              inverse = false
              realValue = random.nextDouble() < probTF
            } else {
              valuation = random.nextDouble() < probTF
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