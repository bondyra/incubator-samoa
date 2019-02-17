package org.apache.samoa.preprocessing.featureselection.topologies.common.filtering;

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

import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;

import java.util.Map;

/**
 * Repository of ranking workers
 */
public class RankingWorkerRepository {
  private Map<Integer, RankingWorker> workers;

  /**
   * Create RankingWorkerRepository instance
   * 
   * @param workers
   *          Map of ranking identifiers and workers that perform ranking computations
   */
  public RankingWorkerRepository(Map<Integer, RankingWorker> workers) {
    this.workers = workers;
  }

  /**
   * Distribute feature sets to designated rankings
   * 
   * @param rankingId
   *          Identifier of the ranking
   * @param numericSet
   *          FeatureSet for numeric attributes
   * @param nominalSet
   *          Feature set for nominal attributes
   * @return True if specified ranking is prepared to be sent
   * @throws IllegalAccessException
   *           If feature sets refer to ranking that is unknown to repository
   */
  public boolean processFeatureSets(int rankingId, FeatureSet numericSet, FeatureSet nominalSet)
      throws IllegalAccessException {
    if (!workers.containsKey(rankingId)) {
      throw new IllegalAccessException("Worker does not exist in repository.");
    }
    RankingWorker worker = workers.get(rankingId);
    return worker.processFeatureSets(numericSet, nominalSet);
  }

  /**
   * Extract event to be sent from underlying worker
   * 
   * @param rankingId
   *          Identifier of ranking
   * @return RankingContentEvent to be sent
   * @throws IllegalAccessException
   *           If requested ranking is unknown to repository
   */
  public RankingContentEvent getEvent(int rankingId) throws IllegalAccessException {
    if (!workers.containsKey(rankingId)) {
      throw new IllegalAccessException("Worker does not exist in repository.");
    }
    return workers.get(rankingId).getEvent();
  }

  /**
   * Handle drift signal
   */
  public void processDrift() {
    for (Map.Entry<Integer, RankingWorker> entry : workers.entrySet()) {
      entry.getValue().processDrift();
    }
  }

  /**
   * Handle warning signal
   */
  public void processWarning() {
    for (Map.Entry<Integer, RankingWorker> entry : workers.entrySet()) {
      entry.getValue().processWarning();
    }
  }
}