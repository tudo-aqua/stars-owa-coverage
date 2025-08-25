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
import org.jgrapht.alg.matching.HopcroftKarpMaximumCardinalityBipartiteMatching
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph
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

  /** Time spent in MaxSAT solver for MaxUnCover. */
  var timeInMaxSAT = mutableListOf(0L)

  /** Time spent in MaxSAT solver for MinUnCover. */
  var timeInMinSAT = mutableListOf(0L)

  /** Time spent in SparseEdmondsMaximumCardinalityMatching solver. */
  var timeInSparseEdmondsMaximumCardinalityMatching = mutableListOf(0L)

  /** Time spent in Hopcroft-Karp Maximum Cardinality Bipartite Matching solver. */
  var timeInHopcroftKarpMaximumCardinalityBipartiteMatching = mutableListOf(0L)

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
      println("Calculating MinUnCover for ${powerLists.size} observed instances...")
      if (minUncoverCount.lastOrNull() == maxSize) {
        minUncoverCount.add(maxSize)
        timeInMinSAT.add(0L)
      } else {
        minUncoverCount.add(calculateMinUnCoverUsingZ3(powerLists))
      }

      // Update maxUnCover
      println("Calculating MaxUnCover for ${powerLists.size} observed instances...")
      if (maxUncoverCount.lastOrNull() == maxSize) {
        maxUncoverCount.add(maxSize)
        timeInSparseEdmondsMaximumCardinalityMatching.add(0L)
        timeInHopcroftKarpMaximumCardinalityBipartiteMatching.add(0L)
        //        timeInMaxSAT.add(0L)
      } else {
        val max1 = calculateMaxUnCoverUsingSparseEdmonds(powerLists)
        val max2 = calculateMaxUnCoverUsingHopcroftKarp(powerLists)
        //        val max3 = calculateMaxUnCoverUsingZ3(powerLists)
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
      val tMinSat = timeInMinSAT.sum() / 1000.0
      val tMaxSat = timeInMaxSAT.sum() / 1000.0
      val tSparse = timeInSparseEdmondsMaximumCardinalityMatching.sum() / 1000.0
      val tHopcroft = timeInHopcroftKarpMaximumCardinalityBipartiteMatching.sum() / 1000.0
      println(
          "Tick: $evaluatedInstances " +
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

  /** Calculates MinUnCover for the observed instances using MaxSAT solver. */
  private fun calculateMinUnCoverUsingZ3(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
    val t0 = System.currentTimeMillis()

    // Initialize Z3 context and optimization solver
    val ctx = Context(mapOf("model" to "true"))
    val opt = ctx.mkOptimize()

    val variables = mutableMapOf<Bitmask, BoolExpr>()

    // Iterate observed instances
    for (instance in powerLists) {
      // Blow up the instance to all possible options by replacing unknowns with both options
      val powerList = instance.second

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
      Status.UNSATISFIABLE -> (-2).also { System.err.println("MaxSat returned UNSATISFIABLE!") }
      Status.UNKNOWN -> (-1).also { System.err.println("MaxSat returned UNKNOWN!") }
    }.also {
      ctx.close()
      timeInMinSAT += (System.currentTimeMillis() - t0)
    }
  }

  /** Calculates MinUnCover for the observed instances using MaxSAT solver. */
  private fun calculateMaxUnCoverUsingZ3(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
    val t0 = System.currentTimeMillis()

    // Initialize Z3 context and optimization solver
    val ctx = Context(mapOf("model" to "true"))
    val opt = ctx.mkOptimize()

    // List of all edges (options) in the bipartite graph
    val edges = mutableListOf<BoolExpr>()

    // Iterate observed instances
    var i = 1
    for (instance in powerLists) {
      // Blow up the instance to all possible options by replacing unknowns with both options
      val powerList = instance.second

      // Create a variable (an edge) for each option
      val options = mutableListOf<BoolExpr>()
      for (option in powerList) {
        options.add(ctx.mkBoolConst("${option}_${i++}"))
      }

      // Add mutual exclusion constraints to ensure only one option (edge) can be true
      for (i in options.indices) {
        for (j in i + 1 until options.size) {
          opt.Add(ctx.mkOr(ctx.mkNot(options[i]), ctx.mkNot(options[j])))
        }
      }

      // Add all options (edges) to the list of edges
      edges.addAll(options)
    }

    // Add soft constraints for maximization
    for (edge in edges) {
      opt.AssertSoft(edge, 1, "")
    }

    // Solve the optimization problem
    return when (opt.Check()) {
      // Return the count of edges that are true in the model
      Status.SATISFIABLE -> edges.count { opt.model.eval(it, true).isTrue }
      Status.UNSATISFIABLE -> (-2).also { System.err.println("MaxSat returned UNSATISFIABLE!") }
      Status.UNKNOWN -> (-1).also { System.err.println("MaxSat returned UNKNOWN!") }
    }.also {
      ctx.close()
      timeInMaxSAT += (System.currentTimeMillis() - t0)
    }
  }

  /** Calculates MaxUnCover for the observed instances. */
  private fun calculateMaxUnCoverUsingSparseEdmonds(
      powerLists: List<Pair<Bitmask, List<Bitmask>>>
  ): Int {
    val t0 = System.currentTimeMillis()

    val (graph, _, _) = createGraph(powerLists)

    // Compute the maximum cardinality matching
    val matching = SparseEdmondsMaximumCardinalityMatching(graph)

    return matching.matching.edges.size.also {
      timeInSparseEdmondsMaximumCardinalityMatching += (System.currentTimeMillis() - t0)
    }
  }

  /** Calculates MaxUnCover for the observed instances. */
  private fun calculateMaxUnCoverUsingHopcroftKarp(
      powerLists: List<Pair<Bitmask, List<Bitmask>>>
  ): Int {
    val t0 = System.currentTimeMillis()

    val (graph, partition1, partition2) = createGraph(powerLists)

    // Compute the maximum cardinality matching
    val matching = HopcroftKarpMaximumCardinalityBipartiteMatching(graph, partition1, partition2)

    return matching.matching.edges.size.also {
      timeInHopcroftKarpMaximumCardinalityBipartiteMatching += (System.currentTimeMillis() - t0)
    }
  }

  /** Creates the bipartite graph for the Maximum Cardinality Matching */
  private fun createGraph(
      powerLists: List<Pair<Bitmask, List<Bitmask>>>
  ): Triple<Graph<String, DefaultEdge>, Set<String>, Set<String>> {
    // Create an undirected simple graph
    val graph: Graph<String, DefaultEdge> = SimpleGraph(DefaultEdge::class.java)

    // Add vertices for the observed instances
    val partition1 = powerLists.map { "o${it.first}" }.toSet()
    partition1.forEach { graph.addVertex(it) }

    // Add vertices for the possible instances
    val partition2 = powerLists.map { it.second }.flatten().map { it.toString() }.toSet()
    partition2.forEach { graph.addVertex(it) }

    // Add edges between observed and possible instances
    for (instance in powerLists) {
      val original = instance.first
      val powerlist = instance.second

      // Add edges from the observed instance containing unknowns to the possible instances
      for (possibleInstance in powerlist) {
        graph.addEdge("o$original", "$possibleInstance")
      }
    }

    return Triple(graph, partition1, partition2)
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
    val xValues = List(timeInMaxSAT.size) { it * sampleSize }

    val values: Map<String, Pair<List<Int>, List<Long>>> =
        mapOf(
            "MinUnCover using Z3" to Pair(xValues, timeInMinSAT),
            //            "MaxUnCover using Z3" to Pair(xValues, timeInMaxSAT),
            "Maximum Cardinality Matching using Sparse Edmonds" to
                Pair(xValues, timeInSparseEdmondsMaximumCardinalityMatching),
            "Maximum Cardinality Matching using Hopcroft-Karp" to
                Pair(xValues, timeInHopcroftKarpMaximumCardinalityBipartiteMatching))

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
