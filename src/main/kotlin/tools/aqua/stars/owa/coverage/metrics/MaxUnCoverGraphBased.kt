package tools.aqua.stars.owa.coverage.metrics

import org.jgrapht.Graph
import org.jgrapht.alg.matching.HopcroftKarpMaximumCardinalityBipartiteMatching
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph

object MaxUnCoverGraphBased {
  /** Time spent in SparseEdmondsMaximumCardinalityMatching solver. */
  var totalTimeInSparseEdmonds = mutableListOf(0L)

  /** Time spent in Hopcroft-Karp Maximum Cardinality Bipartite Matching solver. */
  var totalTimeInHopcroftKarp = mutableListOf(0L)

  /** Calculates MaxUnCover for the observed instances using Sparse Edmonds Maximum Cardinality Matching. */
  fun calculateSparseEdmonds(
    powerLists: List<Pair<Bitmask, List<Bitmask>>>
  ): Int {
    val t0 = System.currentTimeMillis()

    val (graph, _, _) = createGraph(powerLists)

    // Compute the maximum cardinality matching
    val matching = SparseEdmondsMaximumCardinalityMatching(graph)

    return matching.matching.edges.size.also {
      totalTimeInSparseEdmonds += (System.currentTimeMillis() - t0)
    }
  }

  /** Calculates MaxUnCover for the observed instances using Hopcroft-Karp Maximum Cardinality Matching. */
  fun calculateHopcroftKarp(
    powerLists: List<Pair<Bitmask, List<Bitmask>>>
  ): Int {
    val t0 = System.currentTimeMillis()

    val (graph, partition1, partition2) = createGraph(powerLists)

    // Compute the maximum cardinality matching
    val matching = HopcroftKarpMaximumCardinalityBipartiteMatching(graph, partition1, partition2)

    return matching.matching.edges.size.also {
      totalTimeInHopcroftKarp += (System.currentTimeMillis() - t0)
    }
  }

  /** Creates the bipartite graph for the Maximum Cardinality Matching */
  private fun createGraph(
    powerLists: List<Pair<Bitmask, List<Bitmask>>>
  ): Triple<Graph<String, DefaultEdge>, Set<String>, Set<String>> {
    // Create an undirected simple graph
    val graph: Graph<String, DefaultEdge> = SimpleGraph(DefaultEdge::class.java)

    // Add vertices for the observed instances
    val partition1 = powerLists.map{ it.first }.mapIndexed { index, instance -> "o_${index}_${instance}" }.toSet()
    partition1.forEach { graph.addVertex(it) }

    // Add vertices for the possible instances
    val partition2 = powerLists.flatMap { instance -> instance.second.map { "$it" } }.toSet()
    partition2.forEach { graph.addVertex(it) }

    // Add edges between observed and possible instances
    powerLists.forEachIndexed { index, instance ->
      val original = instance.first
      val powerlist = instance.second

      // Add edges from the observed instance containing unknowns to the possible instances
      for (possibleInstance in powerlist) {
        graph.addEdge("o_${index}_${original}", "$possibleInstance")
      }
    }

    return Triple(graph, partition1, partition2)
  }
}