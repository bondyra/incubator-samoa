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

/**
 * Batch ranking worker
 */
public class BatchRankingWorker implements RankingWorker {
  private final int rankingIdentifier;
  private IncrementalRanker numericRanker;
  private IncrementalRanker nominalRanker;
  private final int batchSize;
  private boolean isBatchFilled = false;
  private boolean isFirstProcessing = true;
  /**
   * Incrementing counter of batch identifiers
   */
  private long batchId = 0;
  // these two values help handling unordered processing of feature sets
  private long highestInstanceNumber = 0;
  private long lowestInstanceNumber;
  /**
   * Computed ranking content event. It is saved due to batch discarding
   */
  private RankingContentEvent preparedRankingContentEvent = null;

  /**
   * Create BatchRankingWorker instance
   * 
   * @param rankingIdentifier
   *          Identifier of ranking that worker computes
   * @param batchSize
   *          Batch size - after processing these many sets ranking will be ready to be sent further
   * @param numericRanker
   *          Numeric attributes ranker
   * @param nominalRanker
   *          Nominal attributes ranker
   */
  public BatchRankingWorker(int rankingIdentifier, int batchSize,
      IncrementalRanker numericRanker, IncrementalRanker nominalRanker) {
    this.rankingIdentifier = rankingIdentifier;
    this.numericRanker = numericRanker;
    this.nominalRanker = nominalRanker;
    this.batchSize = batchSize;
  }

  public boolean processFeatureSets(FeatureSet numericSet, FeatureSet nominalSet) throws IllegalStateException {
    long numericInstanceNumber = numericSet.getInstanceNumber();
    long nominalInstanceNumber = nominalSet.getInstanceNumber();
    if (numericInstanceNumber != nominalInstanceNumber)
      throw new IllegalStateException("Numeric and nominal sets come from different instances.");
    if (numericInstanceNumber <= 0)
      throw new IllegalStateException("Instance number is not positive.");

    if (numericInstanceNumber < highestInstanceNumber) {
      // currently processing newer instance, so forget about input values. wont happen if FIFO order is ensured
      return false;
    }
    if (isFirstProcessing) {
      // resolve start instance number
      lowestInstanceNumber = numericInstanceNumber - 1;
      isFirstProcessing = false;
    }

    boolean batchPrepared = false;
    if (numericInstanceNumber > highestInstanceNumber) {
      // new instance has arrived
      if (isBatchFilled) {
        // create new ranking content event and save it, proceed to start creating new batch
        startNewBatch();
        isBatchFilled = false;
        batchPrepared = true;
      }

      highestInstanceNumber = numericInstanceNumber;
      if ((highestInstanceNumber - lowestInstanceNumber) % batchSize == 0) {
        // ranking content event will be computed after arrival of new instance
        isBatchFilled = true;
      }
    }

    this.nominalRanker.process(nominalSet);
    this.numericRanker.process(numericSet);
    return batchPrepared;
  }

  public void processWarning() {
    //no adaptation assumed
  }

  public void processDrift() {
    //no adaptation assumed
  }

  public RankingContentEvent getEvent() throws IllegalStateException {
    if (preparedRankingContentEvent == null) {
      throw new IllegalStateException("No batch was prepared to this time.");
    }
    return preparedRankingContentEvent;
  }

  private void prepareEvent() {
    // compute exact ranking values of attributes if they are outdated
    if (!nominalRanker.isUpdated())
      nominalRanker.update();
    if (!numericRanker.isUpdated())
      numericRanker.update();
    preparedRankingContentEvent = new RankingContentEvent(
        nominalRanker.getRanking(),
        numericRanker.getRanking(),
        getRankingIdentifier(),
        batchId);
  }

  private void startNewBatch() {
    prepareEvent();
    batchId++;
    numericRanker.reset();
    nominalRanker.reset();
  }

  public void reset() {
    numericRanker.reset();
    nominalRanker.reset();
    isFirstProcessing = true;
    isBatchFilled = false;
  }

  public int getRankingIdentifier() {
    return rankingIdentifier;
  }
}
