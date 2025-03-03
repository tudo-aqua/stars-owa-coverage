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

import org.jgrapht.Graph
import org.jgrapht.alg.matching.SparseEdmondsMaximumCardinalityMatching
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleGraph

fun main() {
  // Create an undirected simple graph
  val graph: Graph<String, DefaultEdge> = SimpleGraph(DefaultEdge::class.java)

  // Add vertices
  listOf("o1", "o2", "o3", "o4").forEach { graph.addVertex(it) }
  listOf("t3t5", "t1t3t5", "t2t3t5", "t1t2t3t5").forEach { graph.addVertex(it) }

  // Add edges
  graph.addEdge("o1", "t3t5")
  graph.addEdge("o2", "t1t3t5")
  graph.addEdge("o3", "t3t5")
  graph.addEdge("o3", "t1t3t5")
  graph.addEdge("o4", "t3t5")
  graph.addEdge("o4", "t1t3t5")
  graph.addEdge("o4", "t2t3t5")
  graph.addEdge("o4", "t1t2t3t5")

  // Compute the maximum cardinality matching
  val matching = SparseEdmondsMaximumCardinalityMatching(graph)

  // Print the matching edges
  println("Maximum Cardinality Matching: ${matching.matching.edges}")
}
