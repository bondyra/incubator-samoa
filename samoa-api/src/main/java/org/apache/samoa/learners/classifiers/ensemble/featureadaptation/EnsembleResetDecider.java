package org.apache.samoa.learners.classifiers.ensemble.featureadaptation;

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

import org.apache.samoa.learners.ResultContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethodCreator;

import java.util.*;

/**
 * Decides about which ensemble members should be reset
 */
public class EnsembleResetDecider {
  private Random random;
  private int gracePeriod;
  private Map<Integer, Integer> excludedLearnerAges;
  private Map<Integer, EvaluationMethod> evaluations;
  /**
   * Used to dynamically create EvaluationMethod instances for new ensemble members
   */
  private EvaluationMethodCreator methodCreator;

  /**
   * Create EnsembleResetDecider instance
   * 
   * @param randomSeed
   *          Random seed to ensure deterministic experimentation behavior
   * @param gracePeriod
   *          How many instances must be processed to let ensemble member be reset again
   */
  public EnsembleResetDecider(int randomSeed, int gracePeriod) {
    this.evaluations = new HashMap<>();
    this.excludedLearnerAges = new HashMap<>();
    this.random = new Random(randomSeed);
    this.gracePeriod = gracePeriod;
  }

  /**
   * Process new predictions and update necessary stats
   * 
   * @param event
   *          New result of prediction process
   * @throws InstantiationException
   */
  public void processResultContentEvent(ResultContentEvent event) throws InstantiationException {
    int index = event.getClassifierIndex();
    if (!evaluations.containsKey(index)) {
      EvaluationMethod newMethod = methodCreator.getMethod();
      evaluations.put(index, newMethod);
    }
    EvaluationMethod method = evaluations.get(index);
    method.update(event.getClassVotes(), event.getInstance());
    // if learner is excluded from resetting, update its age and "un-exclude" if it is mature enough
    if (excludedLearnerAges.containsKey(index)) {
      int currentAge = excludedLearnerAges.get(index);
      if (currentAge + 1 >= gracePeriod)
        excludedLearnerAges.remove(index);
      else
        excludedLearnerAges.put(index, currentAge + 1);
    }
  }

  /**
   * @return List of ensemble member identifiers to be reset
   */
  public Collection<Integer> getLearnerIdsToReset() {
    // create learner priorities list
    LearnerPriority[] priorities = new LearnerPriority[evaluations.size()];
    int id = 0;
    for (Map.Entry<Integer, EvaluationMethod> entry : evaluations.entrySet()) {
      priorities[id] = new LearnerPriority(entry.getKey(), entry.getValue().getScore());
      id++;
    }
    // sort ascending by learner grades
    Arrays.sort(priorities);
    // choose learners to reset
    Collection<Integer> learnerIdsToReset = new LinkedList<>();
    for (int i = 0; i < priorities.length; i++) {
      int learnerId = priorities[i].getLearnerId();
      if (excludedLearnerAges.containsKey(learnerId))
        continue;
      if (random.nextDouble() < getResetProbability(i, evaluations.size())) {
        learnerIdsToReset.add(learnerId);
      }
    }
    return learnerIdsToReset;
  }

  /**
   * Reset stats of specified ensemble members. Exclude them temporarily from resetting according to configured
   * gracePeriod
   * 
   * @param learnerIds
   *          Ensemble members identifiers to reset
   * @throws IllegalAccessException
   *           If requested ensemble members to reset are unknown
   */
  public void resetLearners(Collection<Integer> learnerIds) throws IllegalAccessException {
    if (!evaluations.keySet().containsAll(learnerIds))
      throw new IllegalAccessException("Invalid learnerIds specified to reset.");

    for (int learnerId : learnerIds) {
      // exclude learner for further resetting
      excludedLearnerAges.put(learnerId, 0);
      // reset learner score
      evaluations.get(learnerId).reset();
    }
  }

  /**
   * Linear function that calculates the probability of specified learner reset Probability is 1 if learner is the worst
   * Probability is 0 if learner is best (never reset) Else, probability is in inverse proportion to learner grade
   * 
   * @param learnerOrder
   *          Position of learner in sorted list of priorities
   * @param learnersCount
   *          Total number of learners
   * @return Probability of reset
   */
  public static double getResetProbability(int learnerOrder, int learnersCount) {
    if (learnersCount == 1)
      return 1d; // always reset if there is only one learner known to be in ensemble
    return (learnersCount - 1d - learnerOrder) / (learnersCount - 1);
  }

  public void setMethodCreator(EvaluationMethodCreator methodCreator) {
    this.methodCreator = methodCreator;
  }
}
