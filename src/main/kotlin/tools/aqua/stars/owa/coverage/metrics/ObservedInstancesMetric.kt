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

import tools.aqua.stars.core.metric.metrics.providers.Plottable
import tools.aqua.stars.core.metric.metrics.providers.TickMetricProvider
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation

typealias Bitmask = List<Valuation>

/** @param sampleSize Number of segments to evaluate before updating the metric. */
@Suppress("DuplicatedCode")
class ObservedInstancesMetric(val sampleSize: Int = 1, val maxSize: Int) :
    TickMetricProvider<NoEntity, UnknownTickData, TickDataUnitSeconds, TickDataDifferenceSeconds>,
    Plottable {
  private var evaluatedInstances: Int = 0
  private val t0 = System.currentTimeMillis()

  /** List of all observed instances including those containing unknowns */
  val observedInstances = mutableSetOf<Bitmask>()

  /** Counts of all observed instances including those containing unknowns. */
  val observedInstanceCount = mutableListOf<Int>()

  /** Counts of all observed certain instances (Lower bound). */
  val certainInstanceCount = mutableListOf<Int>()

  /** Counts of all possible instances by replacing unknowns with both options (Upper bound). */
  val possibleInstanceCount = mutableListOf<Int>()

  /** Counts of all observed instances with their real value. */
  val realValueInstanceCount = mutableListOf<Int>()

  /** Counts of all observed scenarios when assuming best possible choice for each unknown. */
  val maxUncoverCount = mutableListOf<Int>()

  /** Counts of all observed scenarios when assuming worst possible choice for each unknown. */
  val minUncoverCount = mutableListOf<Int>()

  val gap: Double
    get() =
        (possibleInstanceCount.last() - certainInstanceCount.last()) /
            possibleInstanceCount.last().toDouble() * 100

  override fun evaluate(tick: UnknownTickData) {
    observedInstances.add(tick.unknownData) // Note: This is a set, so duplicates are ignored
    evaluatedInstances++

    // Update "raw" observedInstanceCount
    observedInstanceCount.add(observedInstances.size)

    if (evaluatedInstances % sampleSize == 0) {
      // Calculate power lists for all observed instances
      val powerLists = observedInstances.map { it to powerList(it) }

      // Update lower bound
      certainInstanceCount.add(calculateLowerBound())

      // Update upper bound
      possibleInstanceCount.add(calculateUpperBound(powerLists))

      // Update realValueInstanceCount
      realValueInstanceCount.add(calculateRealValue())

      // Update minUnCover
      if (minUncoverCount.lastOrNull() == maxSize) {
        minUncoverCount.add(maxSize)
        MinUnCoverZ3.totalTime.add(0L)
      } else {
        minUncoverCount.add(MinUnCoverZ3.calculate(powerLists))
      }

      // Update maxUnCover
      if (maxUncoverCount.lastOrNull() == maxSize) {
        maxUncoverCount.add(maxSize)
        MaxUnCoverGraphBased.totalTimeInSparseEdmonds.add(0L)
        MaxUnCoverGraphBased.totalTimeInHopcroftKarp.add(0L)
        //        timeInMaxSAT.add(0L)
      } else {
        val max1 = MaxUnCoverGraphBased.calculateSparseEdmonds(powerLists)
        val max2 = MaxUnCoverGraphBased.calculateHopcroftKarp(powerLists)
        val max3 = MaxUnCoverZ3.calculate(powerLists)
        check(max1 == max2) {
          "MaxUnCover using SparseEdmonds and Hopcroft-Karp should return the same result, but got $max1 and $max2"
        }
        //        check(max1 == max3) {
        //          "MaxUnCover using Graph algorithm and SAT solver should return the same result,
        // but got $max1 and $max3"
        //        }
        maxUncoverCount.add(max1)
      }

      // Log current status
      val t = (System.currentTimeMillis() - t0) / 1000.0
      val tMinSat = MinUnCoverZ3.totalTime.sum() / 1000.0
      val tMaxSat = MaxUnCoverZ3.totalTime.sum() / 1000.0
      val tSparse = MaxUnCoverGraphBased.totalTimeInSparseEdmonds.sum() / 1000.0
      val tHopcroft = MaxUnCoverGraphBased.totalTimeInHopcroftKarp.sum() / 1000.0
      println(
          "\rTick: $evaluatedInstances " +
              "| UB: ${possibleInstanceCount.last()} " +
              "| MaxUC: ${maxUncoverCount.last()} " +
              "| Real: ${realValueInstanceCount.last()} " +
              "| MinUC: ${minUncoverCount.last()} " +
              "| LB: ${certainInstanceCount.last()} " +
              "| Max: $maxSize " +
              "  ||   Gap: ${String.format("%.2f", gap)} % " +
              "  ||   Time: ${String.format("%.2f", t)} s   " +
              "| Time in MinUnCover (SAT): $tMinSat s (${String.format("%.2f", tMinSat * 100 / t)} %) " +
              "| Time in MaxUnCover (Sparse / Hopcroft-Karp / SAT): $tSparse s (${String.format("%.2f", tSparse * 100 / t)} %) / $tHopcroft s (${String.format("%.2f", tHopcroft * 100 / t)} %) / $tMaxSat s (${String.format("%.2f", tMaxSat * 100 / t)} %)")
    }
  }

  /**
   * Calculates the lower bound of the observed instances by filtering only instances with no
   * unknowns.
   */
  private fun calculateLowerBound(): Int = observedInstances.filter { !it.containsUnknown() }.size

  /**
   * Calculates the upper bound of the observed instances by replacing unknowns with both options.
   */
  private fun calculateUpperBound(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int =
      powerLists.map { it.second }.flatten().toSet().size

  /** Calculates the count of distinct observed instances by their real values. */
  private fun calculateRealValue(): Int =
      observedInstances.map { it.map { t -> t.realValue } }.toSet().size

  /** Checks if the given List of conditions contains any unknowns. */
  private fun Bitmask.containsUnknown(): Boolean = any { it.isUnknown }

  /**
   * Replaces all unknowns in the given instance with both options and returns a list of all
   * possible instances.
   */
  private fun powerList(instance: Bitmask): List<Bitmask> {
    var powerList = mutableListOf(instance)

    // Iterate over all unknowns in the instance and replace them with both options
    while (powerList.first().containsUnknown()) {
      val unknownIndex = powerList.first().indexOfFirst { it.isUnknown }

      val newPowerList = mutableListOf<Bitmask>()
      for (item in powerList) {
        newPowerList.add(
            item.toMutableList().apply {
              this[unknownIndex] =
                  Valuation(
                      condition = true,
                      inverseCondition = false,
                      realValue = this[unknownIndex].realValue)
            })
        newPowerList.add(
            item.toMutableList().apply {
              this[unknownIndex] =
                  Valuation(
                      condition = false,
                      inverseCondition = true,
                      realValue = this[unknownIndex].realValue)
            })
      }

      powerList = newPowerList
    }

    return powerList
  }

  override fun writePlots() {
    plotData()
    plotSolverTime()
  }

  private fun plotData() {
    val xValues = List(certainInstanceCount.size) { it * sampleSize }

    val values: Map<String, Pair<List<Int>, List<Int>>> =
        mapOf(
            // "Upper Bound" to ,
            "Observed Possible Instances (Upper bound)" to Pair(xValues, possibleInstanceCount),
            "Observed Instances (MaxUnCover)" to Pair(xValues, maxUncoverCount),
            "Observed real values" to Pair(xValues, realValueInstanceCount),
            "Observed Instances (MinUnCover)" to Pair(xValues, minUncoverCount),
            "Observed Certain Instances (Lower bound)" to Pair(xValues, certainInstanceCount))

    repeat(4) {
      val logX = it % 2 == 1
      val logY = it >= 2

      plotDataAsLineChart(
          plot =
              getPlot(
                  nameToValuesMap = values,
                  xAxisName = "Ticks",
                  yAxisName = "Instance Count",
                  legendHeader = "Legend"),
          logScaleX = logX,
          logScaleY = logY,
          fileName = "plot${if (logX) "_logX" else ""}${if (logY) "_logY" else ""}",
          folder = "ObservedInstancesMetric",
      )
    }
  }

  private fun plotSolverTime() {
    val xValues = List(MinUnCoverZ3.totalTime.size) { it * sampleSize }

    val values: Map<String, Pair<List<Int>, List<Long>>> =
        mapOf(
            "MinUnCover using Z3" to Pair(xValues, MinUnCoverZ3.totalTime),
            //            "MaxUnCover using Z3" to Pair(xValues, timeInMaxSAT),
            "Maximum Cardinality Matching using Sparse Edmonds" to
                Pair(xValues, MaxUnCoverGraphBased.totalTimeInSparseEdmonds),
            "Maximum Cardinality Matching using Hopcroft-Karp" to
                Pair(xValues, MaxUnCoverGraphBased.totalTimeInHopcroftKarp))

    repeat(4) {
      val logX = it % 2 == 1
      val logY = it >= 2

      plotDataAsLineChart(
          plot =
              getPlot(
                  nameToValuesMap = values,
                  xAxisName = "Ticks",
                  yAxisName = "Time in ms",
                  legendHeader = "Legend"),
          logScaleX = logX,
          logScaleY = logY,
          fileName = "timePlot${if (logX) "_logX" else ""}${if (logY) "_logY" else ""}",
          folder = "ObservedInstancesMetric",
      )
    }
  }

  override fun writePlotDataCSV() {}
}
