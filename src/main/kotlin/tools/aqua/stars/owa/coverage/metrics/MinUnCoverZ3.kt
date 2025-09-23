package tools.aqua.stars.owa.coverage.metrics

import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Status

object MinUnCoverZ3 {

  /** Time spent in MaxSAT solver for MinUnCover. */
  var totalTime = mutableListOf(0L)

  /** Calculates MinUnCover for the observed instances using MaxSAT solver. */
  fun calculate(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
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
      totalTime += (System.currentTimeMillis() - t0)
    }
  }
}