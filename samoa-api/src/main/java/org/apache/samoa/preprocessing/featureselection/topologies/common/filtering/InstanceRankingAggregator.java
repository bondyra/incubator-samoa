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

/**
 * Aggregates ranking content events to single instance ranking
 */
public class InstanceRankingAggregator {
  /**
   * Object that aggregates numeric and nominal rankings
   */
  private InstanceRanking instanceRanking = new InstanceRanking();
  private int eventsProcessed = 0;

  /**
   * Aggregate event
   * @param event RankingContentEvent
   */
  public void processEvent(RankingContentEvent event) {
    instanceRanking.updateNumericRanking(event.getNumericRanking());
    instanceRanking.updateNominalRanking(event.getNominalRanking());
    eventsProcessed++;
  }

  public int getEventsProcessed() {
    return eventsProcessed;
  }

  public InstanceRanking getInstanceRanking() {
    return instanceRanking;
  }
}
