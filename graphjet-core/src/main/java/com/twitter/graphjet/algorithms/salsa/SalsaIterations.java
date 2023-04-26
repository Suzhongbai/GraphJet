/**
 * Copyright 2016 Twitter. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
总的来说这个文件用来实现SALSA的逻辑
但具体怎么实现的，我还没有看懂
一个SalsaIterations类，里面写了三个函数，分别是runSalsaIterations、seedLeftSideForFirstIteration和resetWithRequest
其中第一个函数runSalsaIterations是从左到右再从右到左随机游走
第二个函数被第一个函数在一开始调用 感觉是随机种子之类的 还有权重啥的
第三个函数就是对新来的请求重置内部状态
2023.4.26
*/

package com.twitter.graphjet.algorithms.salsa;

import java.util.Random;

import com.google.common.annotations.VisibleForTesting;

// slf4j是接口，定义了8个级别的log优先级从高到低依次为：OFF、FATAL、ERROR、WARN、INFO、DEBUG、TRACE、 ALL。
import org.slf4j.Logger;
//SLF4J获取logger的方式是通过LoggerFactory，LoggerFactory主要是用来打印日志的
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

//fastutil扩展了 Java集合框架，通过提供特定类型的map、set、list和queue
//以及小内存占用、快速访问和插入；也提供大（64位）array、set 和 list，以及快速、实用的二进制文件和文本文件的I/O类
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class implements the logic of SALSA iterations.
 */
// 这个类实现了SALAS的迭代的逻辑
public class SalsaIterations<T extends LeftIndexedBipartiteGraph> {
  //使用指定类初始化日志对象，在日志输出的时候，可以打印出日志信息所在类
  private static final Logger LOG = LoggerFactory.getLogger("graph");
  
  private final CommonInternalState<T> salsaInternalState;
  private final SalsaStats salsaStats;
  private final SingleSalsaIteration leftSalsaIteration;
  private final SingleSalsaIteration rightSalsaIteration;
  private final SingleSalsaIteration finalSalsaIteration;

  /**
   * Initialize state needed to run SALSA iterations by plugging in different kinds of iterations.
   *
   * @param salsaInternalState   is the input state for the iterations to run
   * @param leftSalsaIteration   contains the logic of running the left-to-right iteration
   * @param rightSalsaIteration  contains the logic of running the right-to-left iteration
   * @param finalSalsaIteration  contains the logic of running the final left-to-right iteration
   */
  public SalsaIterations(
      CommonInternalState<T> salsaInternalState,
      SingleSalsaIteration leftSalsaIteration,
      SingleSalsaIteration rightSalsaIteration,
      SingleSalsaIteration finalSalsaIteration) {
    this.salsaInternalState = salsaInternalState;
    this.salsaStats = salsaInternalState.getSalsaStats();
    this.leftSalsaIteration = leftSalsaIteration;
    this.rightSalsaIteration = rightSalsaIteration;
    this.finalSalsaIteration = finalSalsaIteration;
  }
  //从查询节点运行多个独立的随机游走 同时进行所有的随机游走 一次一步
  // 从左边开始 随机游走一步 然后再从右边开始 随机游走一步
  // 算法保存了访问右边节点的次数 之后被用来从结果集中选择top个节点
  /**
   * Main entry point to run the SALSA iterations. We do a monte-carlo implementation of the SALSA
   * algorithm in that we run multiple independent random walks from the queryNode, which then
   * implies that the # visits to nodes can be used for weighting. The particular implementation
   * here actually progresses all of the random walks simultaneously one step at a time. Thus, we
   * start on the left, run one step of the random walk for all walks, then start on the right, run
   * one step of the random walk for all walks and so on. The algorithm maintains visit counters
   * for nodes on the right, which are later used for picking top nodes in
   * {@link SalsaSelectResults}.
   *
   * @param salsaRequest        is the new incoming salsa request
   * @param random              is used for making all the random choices in SALSA
   */
  // 定义 runSalsaIterations函数 两个参数 一个是请求 一个是随机数
  // 这个函数是什么功能？ 随机游走
  public void runSalsaIterations(SalsaRequest salsaRequest, Random random) {
    // 开始重置内部状态
    LOG.info("SALSA: starting to reset internal state");
    resetWithRequest(salsaRequest, random);
    LOG.info("SALSA: done resetting internal state");
    //生成第一次迭代的随机种子
    seedLeftSideForFirstIteration();
    LOG.info("SALSA: done seeding");
    boolean isForwardIteration = true;
    SingleSalsaIteration singleSalsaIteration = leftSalsaIteration;
    
    for (int i = 0; i < salsaInternalState.getSalsaRequest().getMaxRandomWalkLength(); i++) {
      // 在随机游走的步数内 细致的逻辑？ 
      if (isForwardIteration) {
        singleSalsaIteration.runSingleIteration();
        singleSalsaIteration = rightSalsaIteration;
      } else {
        // 为什么是小于这个数？
        if (i < salsaInternalState.getSalsaRequest().getMaxRandomWalkLength() - 2) {
          singleSalsaIteration.runSingleIteration();
          singleSalsaIteration = leftSalsaIteration;
        } else {
          singleSalsaIteration.runSingleIteration();
          singleSalsaIteration = finalSalsaIteration;
        }
      }
      // 来回横跳 
      isForwardIteration = !isForwardIteration;
    }
  }
  
  // 这个函数是什么功能？ 在runSalsaIterations函数开始的时候被调用了
  @VisibleForTesting
  protected void seedLeftSideForFirstIteration() {
    //从请求中获得要查询的节点
    long queryNode = salsaInternalState.getSalsaRequest().getQueryNode();
    //setNumDirectNeighbors这个方法是干嘛的？
    salsaStats.setNumDirectNeighbors(
        salsaInternalState.getBipartiteGraph().getLeftNodeDegree(queryNode));

    Long2DoubleMap seedNodesWithWeight =
        salsaInternalState.getSalsaRequest().getLeftSeedNodesWithWeight();
    LongSet nonZeroSeedSet = salsaInternalState.getNonZeroSeedSet();

    double totalWeight = 0.0;
    for (Long2DoubleMap.Entry entry : seedNodesWithWeight.long2DoubleEntrySet()) {
      // 和节点的度有关？
      if (salsaInternalState.getBipartiteGraph().getLeftNodeDegree(entry.getLongKey())
          > 0) {
        totalWeight += entry.getDoubleValue();
        nonZeroSeedSet.add(entry.getLongKey());
      }
    }

    // If there is a pre-specified weight, we let it take precedence, but if not, then we reset
    // weights in accordance with the fraction of weight requested for the query node.
    // 如果有一个预先指定的权重，我们让它优先，但如果没有，那么我们根据查询节点请求的权重分数重置权重。
    if (!seedNodesWithWeight.containsKey(queryNode)
        && salsaInternalState.getBipartiteGraph().getLeftNodeDegree(queryNode) > 0) {
      double queryNodeWeight = 1.0;
      if (totalWeight > 0.0) {
        queryNodeWeight =
            totalWeight * salsaInternalState.getSalsaRequest().getQueryNodeWeightFraction()
                / (1.0 - salsaInternalState.getSalsaRequest().getQueryNodeWeightFraction());
      }
      seedNodesWithWeight.put(queryNode, queryNodeWeight);
      totalWeight += queryNodeWeight;
      nonZeroSeedSet.add(queryNode);
    }

    for (long leftNode : nonZeroSeedSet) {
      int numWalksToStart = (int) Math.ceil(
          seedNodesWithWeight.get(leftNode) / totalWeight
              * salsaInternalState.getSalsaRequest().getNumRandomWalks());
        salsaInternalState.getCurrentLeftNodes().put(leftNode, numWalksToStart);
    }

    salsaStats.setNumSeedNodes(salsaInternalState.getCurrentLeftNodes().size());
  }

  /**
   * Resets all internal state to answer new incoming request.
   *
   * @param salsaRequest        is the new incoming salsa request
   * @param random              is used for making all the random choices in SALSA
   */
  @VisibleForTesting
  // 对于新来的请求重置内部状态
  protected void resetWithRequest(SalsaRequest salsaRequest, Random random) {
    salsaInternalState.resetWithRequest(salsaRequest);
    leftSalsaIteration.resetWithRequest(salsaRequest, random);
    rightSalsaIteration.resetWithRequest(salsaRequest, random);
    finalSalsaIteration.resetWithRequest(salsaRequest, random);
  }
}
