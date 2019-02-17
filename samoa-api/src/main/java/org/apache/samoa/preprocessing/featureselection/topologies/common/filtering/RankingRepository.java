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

import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository of rankings
 */
public class RankingRepository {
  private Map<Integer, InstanceRankingAggregator> rankingAggregators;

  /**
   * Create RankingRepository instance
   */
  public RankingRepository() {
    this.rankingAggregators = new HashMap<>();
  }

  /**
   * Aggregate event
   * 
   * @param event
   *          Event with ranking results
   */
  public void aggregateEvent(RankingContentEvent event) {
    int rankingId = event.getRankingIdentifier();
    if (!rankingAggregators.containsKey(rankingId)) {
      rankingAggregators.put(rankingId, new InstanceRankingAggregator());
    }
    rankingAggregators.get(rankingId).processEvent(event);
  }

  /**
   * @param rankingId
   *          Identifier of ranking
   * @return Specified ranking
   * @throws IllegalAccessException
   *           If requested ranking does not exist in repository
   */
  public InstanceRanking getInstanceRanking(int rankingId) throws IllegalAccessException {
    if (!rankingAggregators.containsKey(rankingId))
      throw new IllegalAccessException("Ranking aggregator does not exist.");
    return rankingAggregators.get(rankingId).getInstanceRanking();
  }

  /**
   * @param rankingId
   *          Identifier of ranking
   * @return Number of processed events for specified ranking
   * @throws IllegalAccessException
   *           If requested ranking does not exist in repository
   */
  public int getEventsProcessed(int rankingId) throws IllegalAccessException {
    if (!rankingAggregators.containsKey(rankingId))
      throw new IllegalAccessException("Ranking aggregator does not exist.");
    return this.rankingAggregators.get(rankingId).getEventsProcessed();
  }

  /**
   * Forget about all rankings
   */
  public void purge() {
    rankingAggregators.clear();
  }
}
