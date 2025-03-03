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

package tools.aqua.stars.owa.coverage.playground

import com.microsoft.z3.*

fun main() {
  // Step 1: Initialize Z3 context and optimization solver
  val ctx = Context(mapOf("model" to "true"))
  val opt = ctx.mkOptimize()

  // Step 2: Define variables
  // Each set is represented as a boolean variable
  val t3t5 = ctx.mkBoolConst("t3t5")
  val t1t3t5 = ctx.mkBoolConst("t1t3t5")
  val t2t3t5 = ctx.mkBoolConst("t2t3t5")
  val t1t2t3t5 = ctx.mkBoolConst("t1t2t3t5")

  // Step 3: Add hard constraints (original CNF clauses)
  opt.Add(ctx.mkOr(t3t5)) // (t3, t5)
  opt.Add(ctx.mkOr(t1t3t5)) // (t1, t3, t5)
  opt.Add(ctx.mkOr(t3t5, t1t3t5)) // ((t3, t5) ∨ (t1, t3, t5))
  opt.Add(ctx.mkOr(t3t5, t2t3t5, t1t3t5, t1t2t3t5)) // Full disjunction

  // Step 4: Add soft constraints for minimization
  // Minimize the use of each set
  opt.AssertSoft(ctx.mkNot(t3t5), 1, "") // ¬(t3, t5)
  opt.AssertSoft(ctx.mkNot(t1t3t5), 1, "") // ¬(t1, t3, t5)
  opt.AssertSoft(ctx.mkNot(t2t3t5), 1, "") // ¬(t2, t3, t5)
  opt.AssertSoft(ctx.mkNot(t1t2t3t5), 1, "") // ¬(t1, t2, t3, t5)

  // Step 5: Solve the optimization problem
  when (opt.Check()) {
    Status.SATISFIABLE -> {
      println("Solution found:")
      val model = opt.model
      println("t3t5: " + model.eval(t3t5, true))
      println("t1t3t5: " + model.eval(t1t3t5, true))
      println("t2t3t5: " + model.eval(t2t3t5, true))
      println("t1t2t3t5: " + model.eval(t1t2t3t5, true))
    }
    Status.UNSATISFIABLE -> println("No solution exists.")
    Status.UNKNOWN -> println("Solver failed to find a solution.")
  }

  // Step 6: Clean up
  ctx.close()
}
