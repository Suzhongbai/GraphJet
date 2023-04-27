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


package com.twitter.graphjet.algorithms.salsa;

import com.google.common.base.Objects;

import com.twitter.graphjet.algorithms.RecommendationStats;
//这个类封装了从 SALSA 执行中捕获的统计信息。这些统计信息在 SALSA 中是内部使用的，但是其输出是用于外部消费的。
/**
 * This class encapsulates statistics captured from the SALSA execution. The stats are used
 * internally in SALSA, but the output of this is meant for external consumption.
 */
public final class SalsaStats extends RecommendationStats {
  //种子节点的数目？ numSeedNodes 后面有介绍 SALSA开始的种子节点数目 因为是包括查询节点的 所以大于等于1
  private int numSeedNodes;

  /**
   * Convenience constructor that initializes all stats to 0
   */
  public SalsaStats() {
    //super 可以理解为是指向自己超（父）类对象的一个指针，而这个超类指的是离自己最近的一个父类。
    //super(参数)：调用父类中的某一个构造函数（应该为构造函数中的第一条语句）。
    super(0, 0, 0, Integer.MAX_VALUE, 0, 0);
    numSeedNodes = 0;
  }

  /**
     此构造函数为测试目的设置统计信息。
   * This constructor sets the stats for testing purpose. We document the stats use here.
   *
   * @param numSeedNodes           is the number of seed nodes that SALSA starts with. Since this
   *                               number includes the query node, it is >= 1
   * @param numDirectNeighbors     is the number of direct neighbors of the query node 查询节点的一阶邻居
   * @param numRightNodesReached   is the number of unique right nodes that were reached by SALSA 通过SALSA能达到的右边的节点的数目
   * @param numRHSVisits           is the total number of visits done by SALSA to nodes on the RHS
   *                               of the bipartite graph RHS是什么？
   * @param minVisitsPerRightNode  is the minimum number of visits to any RHS node in SALSA
   * @param maxVisitsPerRightNode  is the maximum number of visits to any RHS node in SALSA
   * @param numRightNodesFiltered  is the number of RHS nodes filtered from the final output
   */
  // 为什么有的是用super 有的是用this
  protected SalsaStats(
      int numSeedNodes,
      int numDirectNeighbors,
      int numRightNodesReached,
      int numRHSVisits,
      int minVisitsPerRightNode,
      int maxVisitsPerRightNode,
      int numRightNodesFiltered) {
    super(
      numDirectNeighbors,
      numRightNodesReached,
      numRHSVisits,
      minVisitsPerRightNode,
      maxVisitsPerRightNode,
      numRightNodesFiltered
    );
    this.numSeedNodes = numSeedNodes;
  }

  public int getNumSeedNodes() {
    return numSeedNodes;
  }

  public void setNumSeedNodes(int numSeedNodes) {
    this.numSeedNodes = numSeedNodes;
  }

  @Override
  public int hashCode() {
    //hashCode方法定义在Object类中，每个对象都有一个默认的散列码，其值为对象的存储地址。
    //在Java中, 我们可以通过hashCode()方法获取对象的哈希码, 哈希码的值就是对象的存储地址, 这个方法在Object类中声明, 因此所有的子类都含有该方法.
    return Objects.hashCode(
        numSeedNodes,
        numDirectNeighbors,
        numRightNodesReached,
        numRHSVisits,
        minVisitsPerRightNode,
        maxVisitsPerRightNode,
        numRightNodesFiltered);
  }

  @Override
  public boolean equals(Object obj) {
   
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    //显式参数命名为otherObject, 需要将它转换成另一个叫做other的变量 
    SalsaStats other = (SalsaStats) obj;
    //检测this与otherObject是否引用同一个对象: if(this == otherObject) return true; —— 存储地址相同, 肯定是同个对象, 直接返回true;
    return
        Objects.equal(numSeedNodes, other.numSeedNodes)
        && Objects.equal(numDirectNeighbors, other.numDirectNeighbors)
        && Objects.equal(numRightNodesReached, other.numRightNodesReached)
        && Objects.equal(numRHSVisits, other.numRHSVisits)
        && Objects.equal(minVisitsPerRightNode, other.minVisitsPerRightNode)
        && Objects.equal(maxVisitsPerRightNode, other.maxVisitsPerRightNode)
        && Objects.equal(numRightNodesFiltered, other.numRightNodesFiltered);
  }

  @Override
  public String toString() {
    //Objects对象提供了toStringHelper()方法，使我们可以更加便捷的实现指定的toString()方法
    return Objects.toStringHelper(this)
        .add("numSeedNodes", numSeedNodes)
        .add("numDirectNeighbors", numDirectNeighbors)
        .add("numRightNodesReached", numRightNodesReached)
        .add("numRHSVisits", numRHSVisits)
        .add("minVisitsPerRightNode", minVisitsPerRightNode)
        .add("maxVisitsPerRightNode", maxVisitsPerRightNode)
        .add("numRightNodesFiltered", numRightNodesFiltered)
        .toString();
  }

  /**
  重置所有状态为0 以便后面使用
   * Resets all internal state kept for stats to enable reuse.
   */
  public void reset() {
    numSeedNodes = 0;
    numDirectNeighbors = 0;
    numRightNodesReached = 0;
    numRHSVisits = 0;
    minVisitsPerRightNode = Integer.MAX_VALUE;
    maxVisitsPerRightNode = 0;
    numRightNodesFiltered = 0;
  }
}
