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

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import kotlin.collections.indexOfFirst
import kotlin.collections.toMutableList
import org.jgrapht.Graph
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.SingleTickSegment
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import tools.aqua.stars.owa.coverage.dataclasses.Valuation

// region typealiases
typealias E = NoEntity

typealias T = UnknownTickData

typealias S = SingleTickSegment

typealias U = TickDataUnitSeconds

typealias D = TickDataDifferenceSeconds

typealias Bitmask = List<Valuation>

// endregion

/** @param sampleSize Number of segments to evaluate before updating the metric. */
class ObservedInstancesMetric(val sampleSize: Int = 1) :
    SegmentMetricProvider<E, T, S, U, D>,
    //  Stateful,
    //  Serializable,
    //  Loggable,
    Plottable {
  private var evaluatedInstances: Int = 0

  /** List of all observed instances including those containing unknowns */
  val observedInstances = mutableSetOf<Bitmask>()

  /** Counts of all observed instances including those containing unknowns. */
  val observedInstanceCount = mutableListOf<Int>()

  /** Counts of all observed certain instances. */
  val certainInstanceCount = mutableListOf<Int>()

  /** Counts of all possible instances by replacing unknowns with both options . */
  val possibleInstanceCount = mutableListOf<Int>()

  /** Counts of all observed scenarios when assuming best possible choice for each unknown. */
  val maxUncoverCount = mutableListOf<Int>()

  /** Counts of all observed scenarios when assuming worst possible choice for each unknown. */
  val minUncoverCount = mutableListOf<Int>()

  override fun evaluate(segment: SingleTickSegment) {
    observedInstances.add(
        segment.tick.unknownData) // Note: This is a set, so duplicates are ignored
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

      // Update minUnCover
      minUncoverCount.add(calculateMinUnCover(powerLists))

      // Update maxUnCover
      maxUncoverCount.add(calculateMaxUnCover(powerLists))

      print(
          "\rSegment $evaluatedInstances | UB: ${possibleInstanceCount.last()} | MaxUC: ${maxUncoverCount.last()} | MinUC: ${minUncoverCount.last()} | LB: ${certainInstanceCount.last()} | Max: $maxSize")
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

  /** Calculates MinUnCover for the observed instances. */
  private fun calculateMinUnCover(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
    // Initialize Z3 context and optimization solver
    val ctx = Context(mapOf("model" to "true"))
    val opt = ctx.mkOptimize()

    val variables = mutableMapOf<Bitmask, BoolExpr>()

    // Iterate observed instances
    for (instance in powerLists) {
      // Blow up the instance to all possible options by replacing unknowns with both options
      var powerList = instance.second

      // Create a disjunction of all options. Reuse existing variables if already created
      val options = mutableListOf<BoolExpr>()
      for (option in powerList) {
        options.add(variables.getOrPut(key = option) { ctx.mkBoolConst(option.toString()) })
      }

      // Add the disjunction of all options
      opt.Add(ctx.mkOr(*options.toTypedArray()))
    }

    // Add soft constraints for minimization
    for (variable in variables.values) {
      opt.AssertSoft(ctx.mkNot(variable), 1, "")
    }

    // Solve the optimization problem
    return when (opt.Check()) {
      // Return the count of variables that are true in the model
      Status.SATISFIABLE -> variables.values.count { opt.model.eval(it, true).isTrue }
      Status.UNSATISFIABLE -> -2
      Status.UNKNOWN -> -1
    }.also { ctx.close() }
  }

  /** Calculates MaxUnCover for the observed instances. */
  private fun calculateMaxUnCover(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
    // Create an undirected simple graph
    val graph: Graph<String, DefaultEdge> = SimpleGraph(DefaultEdge::class.java)

    // Add vertices for the observed instances
    powerLists.map { it.first }.forEach { graph.addVertex("o$it") }

    // Add vertices for the possible instances
    powerLists.map { it.second }.flatten().toSet().forEach { graph.addVertex("$it") }

    // Add edges between observed and possible instances
    for (instance in powerLists) {
      val original = instance.first
      val powerlist = instance.second

      // Add edges from the observed instance containing unknowns to the possible instances
      for (possibleInstance in powerlist) {
        graph.addEdge("o$original", "$possibleInstance")
      }
    }

    // Compute the maximum cardinality matching
    val matching = SparseEdmondsMaximumCardinalityMatching(graph)

    return matching.matching.edges.size
  }

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
              this[unknownIndex] = Valuation(true, false, this[unknownIndex].realValue)
            })
        newPowerList.add(
            item.toMutableList().apply {
              this[unknownIndex] = Valuation(false, true, this[unknownIndex].realValue)
            })
      }

      powerList = newPowerList
    }

    return powerList
  }

  override fun writePlots() {
    val xValues = List(certainInstanceCount.size) { it * sampleSize }
    val valuesWithObserved: Map<String, Pair<List<Int>, List<Int>>> =
        mapOf(
            // "Upper Bound" to ,
            "Observed Instances" to
                Pair(
                    xValues,
                    observedInstanceCount.filterIndexed { index, _ -> index % sampleSize == 0 }),
            "Observed Certain Instances (Lower bound)" to Pair(xValues, certainInstanceCount),
            "Observed Possible Instances (Upper bound)" to Pair(xValues, possibleInstanceCount),
            "Observed Instances (MaxUnCover)" to Pair(xValues, maxUncoverCount),
            "Observed Instances (MinUnCover)" to Pair(xValues, minUncoverCount))

    val valuesWithoutObserved: Map<String, Pair<List<Int>, List<Int>>> =
        mapOf(
            // "Upper Bound" to ,
            "Observed Certain Instances (Lower bound)" to Pair(xValues, certainInstanceCount),
            "Observed Possible Instances (Upper bound)" to Pair(xValues, possibleInstanceCount),
            "Observed Instances (MaxUnCover)" to Pair(xValues, maxUncoverCount),
            "Observed Instances (MinUnCover)" to Pair(xValues, minUncoverCount))

    plotDataAsLineChart(
        plot =
            getPlot(
                nameToValuesMap = valuesWithObserved,
                xAxisName = "Segments",
                yAxisName = "Instance Count",
                legendHeader = "Legend"),
        fileName = "plot_with_observed",
        folder = "ObservedInstancesMetric",
    )

    plotDataAsLineChart(
        plot =
            getPlot(
                nameToValuesMap = valuesWithoutObserved,
                xAxisName = "Segments",
                yAxisName = "Instance Count",
                legendHeader = "Legend"),
        fileName = "plot_without_observed",
        folder = "ObservedInstancesMetric",
    )
  }

  override fun writePlotDataCSV() {}
}
