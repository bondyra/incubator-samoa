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

import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Incremental ranking worker that handles drift signals and provides adaptation mechanism
 */
public class IncrementalRankingWorker implements RankingWorker {
  private final int rankingIdentifier;
  /**
   * Alternative and main numeric attributes rankers
   */
  private List<IncrementalRanker> numericRankers;
  /**
   * Alternative and main nominal attributes rankers
   */
  private List<IncrementalRanker> nominalRankers;

  private boolean isAlternativeRanking = false;
  /**
   * Helpful identifier that shortens implementation of ranking switching. Oscillates between 0 and 1.
   */
  private int primaryIndex = 0;
  /**
   * Incrementing value that indicates how many sets have been processed after receiving a warning signal
   */
  private int processedFeatureSetsAfterWarning = 0;
  private final int alternativeRankingPeriod;
  /**
   * Incrementing value that indicates how many sets have been processed
   */
  private long processedFeatureSets = 0;
  private final int delay;

  /**
   * Create IncrementalRankingWorker instance
   * 
   * @param rankingIdentifier
   *          Identifier of ranking that worker computes
   * @param numericRankerA
   *          Main numeric attributes ranker
   * @param nominalRankerA
   *          Main nominal attributes ranker
   * @param numericRankerB
   *          Alternative numeric attributes ranker
   * @param nominalRankerB
   *          Alternative nominal attributes ranker
   * @param alternativeRankingPeriod
   *          Limit of number of processed sets after which alternative ranking is discarded
   * @param delay
   *          Limit of number of processed feature sets after which ranking is ready to be sent further
   */
  public IncrementalRankingWorker(int rankingIdentifier, IncrementalRanker numericRankerA,
      IncrementalRanker nominalRankerA,
      IncrementalRanker numericRankerB, IncrementalRanker nominalRankerB,
      int alternativeRankingPeriod, int delay) {
    this.rankingIdentifier = rankingIdentifier;
    this.numericRankers = Arrays.asList(numericRankerA, numericRankerB);
    this.nominalRankers = Arrays.asList(nominalRankerA, nominalRankerB);
    this.alternativeRankingPeriod = alternativeRankingPeriod;
    this.delay = delay;
  }

  public boolean processFeatureSets(FeatureSet numericSet, FeatureSet nominalSet) throws IllegalStateException {
    if (isAlternativeRanking) {
      this.processedFeatureSetsAfterWarning++;
      if (processedFeatureSetsAfterWarning > alternativeRankingPeriod) {
        // when predefined number of instances have been seen, alternative ranking can be removed
        stopAlternativeRanking();
      } else {
        this.nominalRankers.get(1 - primaryIndex).process(nominalSet);
        this.numericRankers.get(1 - primaryIndex).process(numericSet);
      }
    }
    this.nominalRankers.get(primaryIndex).process(nominalSet);
    this.numericRankers.get(primaryIndex).process(numericSet);

    processedFeatureSets++;
    return processedFeatureSets % delay == 0;
  }

  public void processWarning() {
    // start computation of alternative ranking
    this.isAlternativeRanking = true;
    this.processedFeatureSetsAfterWarning = 0;
  }

  public void processDrift() {
    // alternative ranking becomes main one from now
    this.processedFeatureSetsAfterWarning = 0;
    this.changeRanking();
  }

  public RankingContentEvent getEvent() {
    // compute exact ranking values of attributes if they are outdated
    if (!nominalRankers.get(primaryIndex).isUpdated())
      nominalRankers.get(primaryIndex).update();
    if (!numericRankers.get(primaryIndex).isUpdated())
      numericRankers.get(primaryIndex).update();
    return new RankingContentEvent(
        nominalRankers.get(primaryIndex).getRanking(),
        numericRankers.get(primaryIndex).getRanking(),
        getRankingIdentifier(),
        0 // unused in incremental ranking
    );
  }

  public void reset() {
    for (IncrementalRanker ranker : numericRankers)
      ranker.reset();
    for (IncrementalRanker ranker : nominalRankers)
      ranker.reset();
    processedFeatureSetsAfterWarning = 0;
    processedFeatureSets = 0;
    isAlternativeRanking = false;
  }

  public int getRankingIdentifier() {
    return rankingIdentifier;
  }

  /**
   * Swap alternative ranking with main one
   */
  private void changeRanking() {
    if (isAlternativeRanking) {
      primaryIndex = 1 - primaryIndex;
      this.stopAlternativeRanking();
    }
  }

  private void stopAlternativeRanking() {
    isAlternativeRanking = false;
    this.nominalRankers.get(1 - primaryIndex).reset();
    this.numericRankers.get(1 - primaryIndex).reset();
  }
}
