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
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationResultContentEvent;

import java.util.*;

/**
 * Repository of evaluation workers that perform wrapper feature selection
 */
public class EvaluationWorkerRepository {
  /**
   * Contains all evaluation workers currently performing operations, identified by evaluation identifier
   */
  private Map<Integer, EvaluationWorker> workers;

  /**
   * Create EvaluationWorkerRepository instance
   */
  public EvaluationWorkerRepository() {
    this.workers = new HashMap<>();
  }

  /**
   * Add new evaluation task
   * 
   * @param evaluationId
   *          Identifier of evaluation
   * @param worker
   *          Evaluation worker that will evaluate selection
   * @throws IllegalAccessException
   *           When evaluation identifier is already taken care of
   */
  public void addWorker(int evaluationId, EvaluationWorker worker) throws IllegalAccessException {
    if (workers.containsKey(evaluationId))
      throw new IllegalAccessException("Worker with specified evaluation ID already exists.");
    workers.put(evaluationId, worker);
  }

  /**
   * Process instance from data stream by updating all evaluation workers
   * 
   * @param instance
   *          Input instance
   * @param isTraining
   *          Indicates if instance is for training purpose
   * @param isTesting
   *          Indicates if instance if for testing purpose
   * @return List of evaluation result messages that can be further send
   */
  public List<EvaluationResultContentEvent> processInstance(Instance instance,
      boolean isTraining, boolean isTesting) {
    Collection<Integer> workersToRemove = new LinkedList<>();
    List<EvaluationResultContentEvent> eventsToSend = new LinkedList<>();

    for (Map.Entry<Integer, EvaluationWorker> entry : this.workers.entrySet()) {
      EvaluationWorker evaluationWorker = entry.getValue();
      boolean completed = evaluationWorker.processInstance(instance, isTraining, isTesting);
      if (completed) {
        double evaluationResult = evaluationWorker.getScore();
        double evaluationUpperBound = evaluationWorker.getUpperBound();
        EvaluationResultContentEvent newEvent = new EvaluationResultContentEvent(
            entry.getKey(), evaluationResult, evaluationUpperBound);
        eventsToSend.add(newEvent);
        workersToRemove.add(entry.getKey());
      }
    }
    for (int workerId : workersToRemove) {
      workers.remove(workerId);
    }
    return eventsToSend;
  }

  /**
   * Forget about all of the evaluations
   */
  public void purge() {
    workers.clear();
  }
}
