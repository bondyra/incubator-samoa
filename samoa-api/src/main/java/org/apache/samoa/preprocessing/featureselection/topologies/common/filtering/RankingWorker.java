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

import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;

/**
 * Interface of ranking worker
 */
public interface RankingWorker {
  /**
   * Update rankings
   * 
   * @param numericSet
   *          Set of numeric attributes
   * @param nominalSet
   *          Set of nominal attributes
   * @return True if ranking is prepared to be sent
   * @throws IllegalStateException
   */
  boolean processFeatureSets(FeatureSet numericSet, FeatureSet nominalSet) throws IllegalStateException;

  /**
   * Handle warning signal
   */
  void processWarning();

  /**
   * Handle drift signal
   */
  void processDrift();

  /**
   * @return Event containing prepared rankings that can be send to proper processors
   */
  RankingContentEvent getEvent();

  /**
   * Reset state of worker
   */
  void reset();

  int getRankingIdentifier();
}
