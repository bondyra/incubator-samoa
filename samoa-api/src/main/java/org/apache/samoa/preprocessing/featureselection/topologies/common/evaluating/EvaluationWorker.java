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

import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;

/**
 * EvaluationWorker - applies wrapper feature selection using incoming instances and defined selection
 */
public class EvaluationWorker {
  private final LocalLearner learner;
  private final FeatureFilter filter;
  private final EvaluationMethod method;
  private final int evaluationPeriod;
  /**
   * Internal incrementing instance counter
   */
  private int instancesProcessed = 0;

  /**
   * Create EvaluationWorker instance
   * 
   * @param learner
   *          Model that will be evaluated
   * @param filter
   *          Feature filter that is able to filter instances according to predefined selection
   * @param method
   *          Method of evaluation result computation
   * @param evaluationPeriod
   *          Upper limit of instances needed to finish evaluation
   */
  public EvaluationWorker(LocalLearner learner, FeatureFilter filter, EvaluationMethod method, int evaluationPeriod) {
    this.learner = learner;
    this.filter = filter;
    this.method = method;
    this.evaluationPeriod = evaluationPeriod;
  }

  /**
   * Process instance acquired from data stream
   * 
   * @param instance
   *          Input instance
   * @param isTesting
   *          Indicates if instance is for testing purpose
   * @param isTraining
   *          Indicates if instance if for training purpose
   * @return True if evaluation is finished according to number of instances processed
   */
  public boolean processInstance(Instance instance, boolean isTesting, boolean isTraining) {
    Instance newInstance = filter.processInstance(instance);
    if (isTesting) {
      double[] votes = learner.getVotesForInstance(newInstance);
      this.method.update(votes, newInstance);
    }
    if (isTraining) {
      this.learner.trainOnInstance(newInstance);
      this.instancesProcessed++;
    }
    return this.instancesProcessed >= this.evaluationPeriod;
  }

  /**
   * @return Evaluation result of finished evaluation
   */
  public double getScore() {
    return this.method.getScore();
  }

  /**
   * @return Upper bound of applied evaluation method
   */
  public double getUpperBound() {
    return this.method.getUpperBound();
  }
}
