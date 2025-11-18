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

class MaxUnCoverZ3 {
  /** Time spent in MaxSAT solver for MaxUnCover. */
  var totalTime = mutableListOf(0L)

  /** Calculates MinUnCover for the observed instances using MaxSAT solver. */
  fun calculate(powerLists: List<Pair<Bitmask, List<Bitmask>>>): Int {
    val t0 = System.currentTimeMillis()

    // Initialize Z3 context and optimization solver
    val ctx = Context(mapOf("model" to "true"))
    val opt = ctx.mkOptimize()

    // Create two maps of the nodes (left and right side of the bipartite graph) to their edges
    // (options)
    // Left side: observed instances -> edges to their options
    val observedScenariosMap = mutableMapOf<Bitmask, MutableList<BoolExpr>>()
    // Right side: edges from different observed instances to the same option
    val optionsMap = mutableMapOf<Bitmask, MutableList<BoolExpr>>()

    // Iterate observed instances
    powerLists.forEachIndexed { index, instance ->
      // Blow up the instance to all possible options by replacing unknowns with both options
      val powerList = instance.second

      // Create a variable (an edge) for each option
      for (option in powerList) {
        // Create boolean variable for the edge
        val expr = ctx.mkBoolConst("${index}_${option}")

        // Map the observed instance to its possible vales (left side)
        observedScenariosMap.getOrPut(instance.first) { mutableListOf() }.add(expr)

        // Add the "edge" to the possibly already existing option (right side)
        optionsMap.getOrPut(option) { mutableListOf() }.add(expr)
      }
    }

    // Add mutual exclusion constraints to ensure only one option (edge) can be true for each seen
    // instance (left side)
    for (options in observedScenariosMap.values) {
      for (i in options.indices) {
        for (j in i + 1 until options.size) {
          opt.Add(ctx.mkOr(ctx.mkNot(options[i]), ctx.mkNot(options[j])))
        }
      }
    }

    // Add mutual exclusion constraints to ensure only one option (edge) can be true for each
    // possible instance (right side)
    for (options in optionsMap.values) {
      for (i in options.indices) {
        for (j in i + 1 until options.size) {
          opt.Add(ctx.mkOr(ctx.mkNot(options[i]), ctx.mkNot(options[j])))
        }
      }
    }

    // Add soft constraints for maximization
    for (options in observedScenariosMap.values) {
      for (edge in options) {
        opt.AssertSoft(edge, 1, "")
      }
    }

    // Solve the optimization problem
    return when (opt.Check()) {
      // Return the count of edges that are true in the model
      Status.SATISFIABLE ->
          optionsMap.map { t -> t.value.count { opt.model.eval(it, true).isTrue } }.sum()
      Status.UNSATISFIABLE -> (-2).also { System.err.println("MaxSat returned UNSATISFIABLE!") }
      Status.UNKNOWN -> (-1).also { System.err.println("MaxSat returned UNKNOWN!") }
    }.also {
      ctx.close()
      totalTime += (System.currentTimeMillis() - t0)
    }
  }
}
