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


package com.twitter.graphjet.algorithms.salsa.fullgraph;

import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;

//继承了LeftSalsaIteration类
// 为推荐构建社会证明？

public class FinalSalsaIteration extends LeftSalsaIteration {
  /**
   * This constructs a left iteration that will also construct social proof for the
   * recommendations.
   *
   * @param salsaInternalState  is the internal state to use
   */
  public FinalSalsaIteration(SalsaInternalState salsaInternalState) {
    //super 调用父类的构造方法 且必须在构造方法的第一行
    //只能出现在子类或者构造方法中
    super(
        salsaInternalState,
        new SalsaNodeVisitor.NodeVisitorWithSocialProof(
            salsaInternalState.getVisitedRightNodes())
        );
  }
}
