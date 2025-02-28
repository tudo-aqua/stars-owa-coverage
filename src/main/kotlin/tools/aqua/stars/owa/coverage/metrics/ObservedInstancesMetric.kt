package tools.aqua.stars.owa.coverage.metrics

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

//region typealiases
typealias E = NoEntity
typealias T = UnknownTickData
typealias S = SingleTickSegment
typealias U = TickDataUnitSeconds
typealias D = TickDataDifferenceSeconds
//endregion

class ObservedInstancesMetric:
  SegmentMetricProvider<E, T, S, U, D>,
//  Stateful,
//  Serializable,
//  Loggable,
  Plottable
{
  /** List of all observed instances including those containing unknowns */
  val observedInstances = mutableSetOf<List<Pair<Boolean, Boolean>>>()

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
  private fun calculateUpperBound(): Int {
    val set = mutableSetOf<List<Pair<Boolean, Boolean>>>()

    for (instance in observedInstances) {
      var powerList = mutableListOf(instance)

      // Iterate over all unknowns in the instance and replace them with both options
      while (powerList.first().containsUnknown()) {
        val unknownIndex = powerList.first().indexOfFirst { !it.first && !it.second }

        val newPowerList = mutableListOf<List<Pair<Boolean, Boolean>>>()
        for (item in powerList) {
          newPowerList.add(item.toMutableList().apply { this[unknownIndex] = Pair(true, false) })
          newPowerList.add(item.toMutableList().apply { this[unknownIndex] = Pair(false, true) })
        }

        powerList = newPowerList
      }

      set.addAll(powerList)
    }

    return set.size
  }

  /**
   * Calculates MinUnCover for the observed instances.
   */
  private fun calculateMinUnCover(): Int {
    return 0  //TODO
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
  private fun List<Pair<Boolean, Boolean>>.containsUnknown(): Boolean = any { !it.first && !it.second }

  override fun writePlots() {
    val xValues = List(observedInstanceCount.size) { it }
    val values: Map<String, Pair<List<Int>, List<Int>>> = mapOf(
      //"Upper Bound" to ,
      "Observed Instances" to Pair(xValues, observedInstanceCount),
      "Observed Certain Instances (Lower bound)" to Pair(xValues, certainInstanceCount),
      "Observed Possible Instances (Upper bound)" to Pair(xValues, possibleInstanceCount),
      "Observed Instances (MaxUnCover)" to Pair(xValues, maxUncoverCount),
      "Observed Instances (MinUnCover)" to Pair(xValues, minUncoverCount)
    )

    plotDataAsLineChart(
      plot = getPlot(
        nameToValuesMap = values,
        xAxisName = "Segments",
        yAxisName = "Instance Count",
        legendHeader = "Legend"),
      fileName = "plot",
      folder = "ObservedInstancesMetric",
    )
  }

  override fun writePlotDataCSV() {}
}