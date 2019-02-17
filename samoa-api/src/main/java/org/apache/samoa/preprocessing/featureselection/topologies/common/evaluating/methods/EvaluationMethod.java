package org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods;

/*
 * #%L
 * SAMOA
 * %%
 * Copyright (C) 2014 - 2018 Apache Software Foundation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.apache.samoa.instances.Instance;

/**
 * Interface of evaluation method used i.a. in wrapper feature selection approaches
 */
public interface EvaluationMethod {
  /**
   * Update incremental evaluation result
   * 
   * @param votes
   *          Standard prediction votes returned from learners
   * @param instance
   *          Instance which was a subject of prediction
   */
  void update(double[] votes, Instance instance);

  /**
   * @return Current evaluation score
   */
  double getScore();

  /**
   * Reset evaluating
   */
  void reset();

  /**
   * @return Static maximum possible evaluation result, for normalization purpose
   */
  double getUpperBound();
}
