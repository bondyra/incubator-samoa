package org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating;

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

import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;

/**
 * EvaluationWorker creator
 */
public class EvaluationWorkerCreator {
  /**
   * Create EvaluationWorkerCreator instance
   */
  public EvaluationWorkerCreator() {
  }

  /**
   * Create EvaluationWorker instance with specific parameters
   * 
   * @param learner
   *          LocalLearner to be used in evaluation worker computations
   * @param filter
   *          FeatureFilter to be used in evaluation worker computations
   * @param method
   *          EvaluationMethod to be used in evaluation worker computations
   * @param evaluationPeriod
   *          Period of evaluation
   * @return EvaluationWorker instance
   */
  public EvaluationWorker getWorker(LocalLearner learner, FeatureFilter filter, EvaluationMethod method,
      int evaluationPeriod) {
    return new EvaluationWorker(learner, filter, method, evaluationPeriod);
  }
}
