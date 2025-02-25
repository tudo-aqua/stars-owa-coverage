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

package tools.aqua.stars.owa.coverage.metrics

//import java.util.logging.Logger
//import kotlin.random.Random
//import tools.aqua.stars.core.metric.metrics.evaluation.ValidTSCInstancesPerTSCMetric
//import tools.aqua.stars.core.metric.providers.Loggable
//import tools.aqua.stars.core.metric.providers.PostEvaluationMetricProvider
//import tools.aqua.stars.core.tsc.TSC
//import tools.aqua.stars.core.tsc.instance.TSCInstance
//import tools.aqua.stars.core.tsc.instance.TSCInstanceNode
//import tools.aqua.stars.core.types.EntityType
//import tools.aqua.stars.core.types.SegmentType
//import tools.aqua.stars.core.types.TickDataType
//import tools.aqua.stars.core.types.TickDifference
//import tools.aqua.stars.core.types.TickUnit
//import tools.aqua.stars.owa.coverage.tsc.TSCUncertainInstanceNode
//
//class UncertainValidTSCInstancesPerTSCMetric<
//    E : EntityType<E, T, S, U, D>,
//    T : TickDataType<E, T, S, U, D>,
//    S : SegmentType<E, T, S, U, D>,
//    U : TickUnit<U, D>,
//    D : TickDifference<D>>(
//    override val dependsOn: ValidTSCInstancesPerTSCMetric<E, T, S, U, D>,
//    val uncertainties: Map<String, Double>,
//    override val loggerIdentifier: String = "uncertainties",
//    override val logger: Logger = Loggable.getLogger(loggerIdentifier)
//) : PostEvaluationMetricProvider<E, T, S, U, D>, Loggable {
//
//  private var uncertainInstances:
//      Map<
//          TSC<E, T, S, U, D>,
//          Map<TSCInstanceNode<E, T, S, U, D>, List<TSCInstance<E, T, S, U, D>>>> =
//      emptyMap()
//
//  override fun postEvaluate() {
//    val validTSCInstances:
//        MutableMap<
//            TSC<E, T, S, U, D>,
//            MutableMap<TSCInstanceNode<E, T, S, U, D>, MutableList<TSCInstance<E, T, S, U, D>>>> =
//        dependsOn.getState()
//    val oldInstances = validTSCInstances.toMap()
//    uncertainInstances =
//        validTSCInstances.mapValues { (_, validInstancesMap) ->
//          validInstancesMap.mapValues { (_, validInstances) ->
//            validInstances.map { validInstance ->
//              val leafNodeEdges = validInstance.rootNode.getLeafNodeEdges(validInstance.rootNode)
//              leafNodeEdges.forEach { leafNodeEdge ->
//                val uncertaintyForCurrentLeafNode =
//                    uncertainties.getOrDefault(leafNodeEdge.destination.label, null)
//                if (uncertaintyForCurrentLeafNode != null) {
//                  val randomValue = Random.nextDouble(0.0, 1.0)
//                  if (randomValue in 0.0..uncertaintyForCurrentLeafNode) {
//                    val uncertainInstanceNode =
//                        TSCUncertainInstanceNode(leafNodeEdge.destination.tscNode)
//                    leafNodeEdge.destination = uncertainInstanceNode
//                  }
//                }
//              }
//              return@map validInstance
//            }
//          }
//        }
//    val countOfUncertainInstances =
//        validTSCInstances.mapValues { (_, instances) ->
//          instances.mapValues { (_, instances) ->
//            instances.sumOf {
//              it.rootNode.getLeafNodeEdges(it.rootNode).count {
//                it.destination is TSCUncertainInstanceNode
//              }
//            }
//          }
//        }
//  }
//
//  override fun printPostEvaluationResult() {
//    uncertainInstances.entries.first().value.forEach { (_, instances) ->
//      instances.forEach { instance ->
//        logFine("Instance:")
//        logFine(instance.rootNode)
//      }
//      logFine()
//    }
//  }
//}
