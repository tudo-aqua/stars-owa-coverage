package tools.aqua.stars.owa.coverage.metrics

import tools.aqua.stars.core.metric.providers.Plottable
import tools.aqua.stars.core.metric.providers.TSCAndTSCInstanceNodeMetricProvider
import tools.aqua.stars.core.metric.utils.getPlot
import tools.aqua.stars.core.metric.utils.plotDataAsLineChart
import tools.aqua.stars.core.tsc.TSC
import tools.aqua.stars.core.tsc.instance.TSCInstance
import tools.aqua.stars.core.tsc.instance.TSCInstanceNode
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
  TSCAndTSCInstanceNodeMetricProvider<E, T, S, U, D>,
//  Stateful,
//  Serializable,
//  Loggable,
  Plottable
{
  /** List of all observed instances including those containing unknowns */
  val observedInstances = mutableSetOf<TSCInstanceNode<E,T,S,U,D>>()

  /** Counts of all observed instances including those containing unknowns. */
  val observedInstanceCount = mutableListOf<Int>()

  /** Counts of all observed certain instances. */
  val observedCertainInstanceCount = mutableListOf<Int>()

  /** Counts of all observed scenarios when assuming best possible choice for each unknown. */
  val observedInstanceMaxUncoverCount = mutableListOf<Int>()

  /** Counts of all observed scenarios when assuming worst possible choice for each unknown. */
  val observedInstanceMinUncoverCount = mutableListOf<Int>()

  override fun evaluate(
    tsc: TSC<E,T,S,U,D>,
    tscInstance: TSCInstance<E,T,S,U,D>
  ) {
    observedInstances.add(tscInstance.rootNode) //Note: This is a set, so duplicates are ignored

    // Update "raw" observedInstanceCount
    observedInstanceCount.add(observedInstances.size)

    // Update lower bound
    observedCertainInstanceCount.add(calculateLowerBound())

    // Update minUnCover
    observedInstanceMinUncoverCount.add(calculateMinUnCover())

    // Update maxUnCover
    observedInstanceMaxUncoverCount.add(calculateMaxUnCover())
  }

  /**
   * Calculates the lower bound of the observed instances by filtering only instances with no unknowns.
   */
  private fun calculateLowerBound(): Int =
    observedInstances.filter { !it.containsUnknown() }.size

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
   * Checks if the given [TSCInstanceNode] contains any unknowns.
   */
  private fun TSCInstanceNode<E,T,S,U,D>.containsUnknown(): Boolean =
    edges.any { it.isUnknown || it.destination.containsUnknown() }

  override fun writePlots() {
    val xValues = List(observedInstanceCount.size) { it }
    val values: Map<String, Pair<List<Int>, List<Int>>> = mapOf(
      //"Upper Bound" to ,
      "Observed Certain Instances (Lower bound)" to Pair(xValues, observedCertainInstanceCount),
      "Observed Instances (MaxUnCover)" to Pair(xValues, observedInstanceMaxUncoverCount),
      "Observed Instances (MinUnCover)" to Pair(xValues, observedInstanceMinUncoverCount)
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