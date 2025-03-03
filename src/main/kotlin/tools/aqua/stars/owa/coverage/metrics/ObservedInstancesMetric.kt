package tools.aqua.stars.owa.coverage.metrics

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status
import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.SegmentMetricProvider
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds
import tools.aqua.stars.owa.coverage.dataclasses.NoEntity
import tools.aqua.stars.owa.coverage.dataclasses.SingleTickSegment
import tools.aqua.stars.owa.coverage.dataclasses.UnknownTickData
import kotlin.collections.indexOfFirst
import kotlin.collections.toMutableList

//region typealiases
typealias E = NoEntity
typealias T = UnknownTickData
typealias S = SingleTickSegment
typealias U = TickDataUnitSeconds
typealias D = TickDataDifferenceSeconds
typealias Bitmask = List<Pair<Boolean, Boolean>>
//endregion

class ObservedInstancesMetric:
  SegmentMetricProvider<E, T, S, U, D>,
//  Stateful,
//  Serializable,
//  Loggable,
  Plottable
{
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

  override fun evaluate(
    segment: SegmentType<E,T,S,U,D>
  ) {
    println("Evaluating segment ${evaluatedInstances++}")

    check(segment is SingleTickSegment) // Required for smart cast

    observedInstances.add(segment.tick.unknownData) //Note: This is a set, so duplicates are ignored

    // Update "raw" observedInstanceCount
    observedInstanceCount.add(observedInstances.size)

    // Update lower bound
    certainInstanceCount.add(calculateLowerBound())

    // Update upper bound
    possibleInstanceCount.add(calculateUpperBound())

    // Update minUnCover
    minUncoverCount.add(calculateMinUnCover())

    // Update maxUnCover
    maxUncoverCount.add(calculateMaxUnCover())
  }

  /**
   * Calculates the lower bound of the observed instances by filtering only instances with no unknowns.
   */
  private fun calculateLowerBound(): Int =
    observedInstances.filter { !it.containsUnknown() }.size

  /**
   * Calculates the upper bound of the observed instances by replacing unknowns with both options.
   */
  private fun calculateUpperBound(): Int = observedInstances.map { powerList(it) }.flatten().toSet().size

  /**
   * Calculates MinUnCover for the observed instances.
   */
  private fun calculateMinUnCover(): Int {
    // Initialize Z3 context and optimization solver
    val ctx = Context(mapOf("model" to "true"))
    val opt = ctx.mkOptimize()

    val variables = mutableMapOf<Bitmask, BoolExpr>()

    // Iterate observed instances
    for (instance in observedInstances) {
      // Blow up the instance to all possible options by replacing unknowns with both options
      var powerList = powerList(instance)

      // Create a disjunction of all options. Reuse existing variables if already created
      val options = mutableListOf<BoolExpr>()
      for(option in powerList) {
        options.add(variables.getOrPut(key=option) { ctx.mkBoolConst(option.toString()) })
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

  /**
   * Calculates MaxUnCover for the observed instances.
   */
  private fun calculateMaxUnCover(): Int {
    return 0  //TODO
  }

  /**
   * Checks if the given List of conditions contains any unknowns.
   */
  private fun Bitmask.containsUnknown(): Boolean = any { !it.first && !it.second }

  /**
   * Replaces all unknowns in the given instance with both options and returns a list of all possible instances.
   */
  private fun powerList(instance : Bitmask): List<Bitmask> {
    var powerList = mutableListOf(instance)

    // Iterate over all unknowns in the instance and replace them with both options
    while (powerList.first().containsUnknown()) {
      val unknownIndex = powerList.first().indexOfFirst { !it.first && !it.second }

      val newPowerList = mutableListOf<Bitmask>()
      for (item in powerList) {
        newPowerList.add(item.toMutableList().apply { this[unknownIndex] = Pair(true, false) })
        newPowerList.add(item.toMutableList().apply { this[unknownIndex] = Pair(false, true) })
      }

      powerList = newPowerList
    }

    return powerList
  }

  override fun writePlots() {
    val xValues = List(observedInstanceCount.size) { it }
    val valuesWithObserved: Map<String, Pair<List<Int>, List<Int>>> = mapOf(
      //"Upper Bound" to ,
      "Observed Instances" to Pair(xValues, observedInstanceCount),
      "Observed Certain Instances (Lower bound)" to Pair(xValues, certainInstanceCount),
      "Observed Possible Instances (Upper bound)" to Pair(xValues, possibleInstanceCount),
      "Observed Instances (MaxUnCover)" to Pair(xValues, maxUncoverCount),
      "Observed Instances (MinUnCover)" to Pair(xValues, minUncoverCount)
    )

    val valuesWithoutObserved: Map<String, Pair<List<Int>, List<Int>>> = mapOf(
      //"Upper Bound" to ,
      "Observed Certain Instances (Lower bound)" to Pair(xValues, certainInstanceCount),
      "Observed Possible Instances (Upper bound)" to Pair(xValues, possibleInstanceCount),
      "Observed Instances (MaxUnCover)" to Pair(xValues, maxUncoverCount),
      "Observed Instances (MinUnCover)" to Pair(xValues, minUncoverCount)
    )

    plotDataAsLineChart(
      plot = getPlot(
        nameToValuesMap = valuesWithObserved,
        xAxisName = "Segments",
        yAxisName = "Instance Count",
        legendHeader = "Legend"),
      fileName = "plot_with_observed",
      folder = "ObservedInstancesMetric",
    )

    plotDataAsLineChart(
      plot = getPlot(
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