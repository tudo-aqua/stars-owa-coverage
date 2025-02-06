/*
 * Copyright 2024-2025 The STARS OWA Coverage Authors
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

package tools.aqua.stars.owa.coverage

import org.sat4j.maxsat.SolverFactory
import org.sat4j.maxsat.WeightedMaxSatDecorator
import org.sat4j.pb.IPBSolver
import org.sat4j.specs.TimeoutException

fun main() {
  // Step 1: Create a MaxSAT solver
  val solver: IPBSolver = SolverFactory.newDefault()
  val maxSatSolver = WeightedMaxSatDecorator(solver)

  // Step 2: Define variables
  // Sat4j variables are 1-indexed; 0 is reserved.
  val t3t5 = 1
  val t1t3t5 = 2
  val t2t3t5 = 3
  val t1t2t3t5 = 4

  // Step 3: Add hard clauses (original CNF constraints)
  try {
    maxSatSolver.newVar(t3t5)
    maxSatSolver.newVar(t1t3t5)
    maxSatSolver.newVar(t2t3t5)
    maxSatSolver.newVar(t1t2t3t5)
    // (t3, t5)
    maxSatSolver.addHardClause(org.sat4j.core.VecInt(intArrayOf(t3t5)))

    // (t1, t3, t5)
    maxSatSolver.addHardClause(org.sat4j.core.VecInt(intArrayOf(t1t3t5)))

    // ((t3, t5) ∨ (t1, t3, t5))
    maxSatSolver.addHardClause(org.sat4j.core.VecInt(intArrayOf(t3t5, t1t3t5)))

    // ((t3, t5) ∨ (t2, t3, t5) ∨ (t1, t3, t5) ∨ (t1, t2, t3, t5))
    maxSatSolver.addHardClause(org.sat4j.core.VecInt(intArrayOf(t3t5, t2t3t5, t1t3t5, t1t2t3t5)))

    // Step 4: Add soft clauses (minimize hitting set)
    // ¬(t3, t5)
    maxSatSolver.addSoftClause(1, org.sat4j.core.VecInt(intArrayOf(-t3t5)))

    // ¬(t1, t3, t5)
    maxSatSolver.addSoftClause(1, org.sat4j.core.VecInt(intArrayOf(-t1t3t5)))

    // ¬(t2, t3, t5)
    maxSatSolver.addSoftClause(1, org.sat4j.core.VecInt(intArrayOf(-t2t3t5)))

    // ¬(t1, t2, t3, t5)
    maxSatSolver.addSoftClause(1, org.sat4j.core.VecInt(intArrayOf(-t1t2t3t5)))

    // Step 5: Solve the MaxSAT problem
    if (maxSatSolver.isSatisfiable) {
      println("Solution found:")
      println("Model: " + maxSatSolver.model().joinToString())
    } else {
      println("No solution exists.")
    }
  } catch (e: TimeoutException) {
    println("Solver timed out.")
  }
}
