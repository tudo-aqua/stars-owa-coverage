package tools.aqua.stars.owa.coverage.metrics

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status

object MaxUnCoverZ3 {
  /** Time spent in MaxSAT solver for MaxUnCover. */
  var totalTime = mutableListOf(0L)

  /** Calculates MinUnCover for the observed instances using MaxSAT solver. */
  fun calculate(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
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
      totalTime += (System.currentTimeMillis() - t0)
    }
  }
}