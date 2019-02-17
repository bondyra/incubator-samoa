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

import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

import java.util.*;

/**
 * Repository of evaluations that helps with evaluation coordination
 */
public class EvaluationRepository {
  /**
   * Contains all evaluations partitioned in groups
   */
  private Map<Integer, List<Evaluation>> evaluationGroups;
  /**
   * Auxiliary structure to speed up access to group id that specified evaluation id belongs to
   */
  private Map<Integer, Integer> groupMapping;
  /**
   * Incrementing evaluation identifier
   */
  private int evaluationId = 0;
  /**
   * Incrementing evaluation group identifier
   */
  private int evaluationGroupId = 0;

  /**
   * Create EvaluationRepository instance
   */
  public EvaluationRepository() {
    evaluationGroups = new HashMap<>();
    groupMapping = new HashMap<>();
  }

  /**
   * Set evaluation result of specified evaluation
   * 
   * @param evaluationId
   *          Identifier of evaluation that is updated
   * @param evaluationResult
   *          Result of evaluation
   * @param evaluationUpperBound
   *          Upper bound of evaluation
   * @return True if all evaluations from evaluation group have results
   * @throws IllegalAccessException
   */
  public boolean updateEvaluationResult(int evaluationId, double evaluationResult, double evaluationUpperBound)
      throws IllegalAccessException {
    boolean groupPrepared = true;
    int evaluationGroupId = this.getGroupByEvaluationId(evaluationId);

    if (!evaluationGroups.containsKey(evaluationGroupId))
      throw new IllegalAccessException("No evaluation group found (possible drift event occurred).");

    Collection<Evaluation> entries = evaluationGroups.get(evaluationGroupId);
    if (entries != null) {
      Evaluation matchingEntry = null;
      for (Evaluation entry : entries) {
        if (entry.getEvaluationId() == evaluationId) {
          matchingEntry = entry;
        } else {
          groupPrepared &= entry.isPrepared();
        }
      }
      if (matchingEntry != null) {
        matchingEntry.setEvaluationResult(evaluationResult);
        matchingEntry.setEvaluationUpperBound(evaluationUpperBound);
      }
    }
    return groupPrepared; // false if any evaluation from group hasn't got a result yet
  }

  /**
   *
   * @param selections
   * @return
   */
  public int createEvaluationGroup(List<Selection> selections) {
    int groupId = evaluationGroupId++;
    List<Evaluation> newEvaluations = new LinkedList<>();
    for (Selection selection : selections) {
      int id = evaluationId++;
      Evaluation newEvaluation = new Evaluation(selection);
      newEvaluation.setEvaluationId(id);
      groupMapping.put(id, groupId);
      newEvaluations.add(newEvaluation);
    }
    evaluationGroups.put(groupId, newEvaluations);
    return groupId;
  }

  /**
   * @param evaluationGroupId
   *          Identifier of group to be acquired
   * @return List of all evaluations from specific group
   * @throws IllegalAccessException
   *           In case of non-existent group
   */
  public List<Evaluation> getEvaluationGroup(int evaluationGroupId) throws IllegalAccessException {
    if (!evaluationGroups.containsKey(evaluationGroupId)) {
      throw new IllegalAccessException("No evaluation group found in repository (possible drift event occurred).");
    }
    return evaluationGroups.get(evaluationGroupId);
  }

  /**
   * @param evaluationGroupId
   *          Identifier of group to be removed
   * @throws IllegalAccessException
   *           In case of non-existent group
   */
  public void removeEvaluationGroup(int evaluationGroupId) throws IllegalAccessException {
    if (!evaluationGroups.containsKey(evaluationGroupId)) {
      throw new IllegalAccessException("No evaluation group found in repository (possible drift event occurred).");
    }
    evaluationGroups.remove(evaluationGroupId);
    Collection<Integer> evaluationIds = new LinkedList<>();
    for (Map.Entry<Integer, Integer> entry : groupMapping.entrySet()) {
      if (entry.getValue() == evaluationGroupId) {
        evaluationIds.add(entry.getKey());
      }
    }
    for (Integer id : evaluationIds) {
      groupMapping.remove(id);
    }
  }

  /**
   * Forget about all evaluations
   */
  public void purge() {
    evaluationGroups.clear();
    groupMapping.clear();
    // there is no reset of incrementing identifiers - because it could be harmful in distributed use cases
  }

  /**
   * @param evaluationId
   *          Identifier of evaluation
   * @return Group identifier that evaluation belongs to
   * @throws IllegalAccessException
   *           In case of non-existent evaluation
   */
  public int getGroupByEvaluationId(int evaluationId) throws IllegalAccessException {
    if (!groupMapping.containsKey(evaluationId))
      throw new IllegalAccessException("No evaluation found in repository (possible drift event occurred).");
    return groupMapping.get(evaluationId);
  }
}
