package org.apache.samoa.preprocessing.featureselection.ranking.incremental;

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

import java.util.*;

/**
 * Incremental ranker that uses information gain measure
 */
public class InformationGainIncrementalRanker extends InformationTheoryIncrementalRanker {
  @Override
  public void update() {
    if (updated)
      return;
    rankingValues = this.getInformationGain();
    updated = true;
  }

  /**
   * Compute information gain for all known features
   * 
   * @return Map of feature index and features' current infromation gain value
   */
  private Map<Integer, Double> getInformationGain() {
    HashMap<ComputeKey, Integer> colSums = new HashMap<>();
    HashMap<ComputeKey, Integer> rowSums = new HashMap<>();
    HashMap<Integer, Integer> sums = new HashMap<>();
    HashMap<Integer, Double> elemLogsSums = new HashMap<>();
    HashMap<Integer, Double> colLogsSums = new HashMap<>();
    HashMap<Integer, Double> rowLogsSums = new HashMap<>();

    for (Map.Entry<Key, Integer> entry : this.contingencyTable.entrySet()) {
      int index = entry.getKey().featureIndex;
      double classValue = entry.getKey().classValue;
      double featureValue = entry.getKey().featureValue;
      ComputeKey columnKey = new ComputeKey(index, classValue);
      ComputeKey rowKey = new ComputeKey(index, featureValue);
      int entryValue = entry.getValue();
      int currentSumColumn = colSums.containsKey(columnKey) ? colSums.get(columnKey) : 0;
      int currentSumRow = rowSums.containsKey(rowKey) ? rowSums.get(rowKey) : 0;
      colSums.put(columnKey, currentSumColumn + entryValue);
      rowSums.put(rowKey, currentSumRow + entryValue);
      double currLogTotal = elemLogsSums.containsKey(index) ? elemLogsSums.get(index) : 0d;
      elemLogsSums.put(index, currLogTotal + xlogx(entryValue));
    }
    for (Map.Entry<ComputeKey, Integer> entry : rowSums.entrySet()) {
      int index = entry.getKey().featureIndex;
      int value = entry.getValue();
      int currTotal = sums.containsKey(index) ? sums.get(index) : 0;
      sums.put(index, currTotal + value);
      double currSum = rowLogsSums.containsKey(index) ? rowLogsSums.get(index) : 0d;
      rowLogsSums.put(index, currSum - xlogx(value));
    }
    for (Map.Entry<ComputeKey, Integer> entry : colSums.entrySet()) {
      int index = entry.getKey().featureIndex;
      int value = entry.getValue();
      double currSum = colLogsSums.containsKey(index) ? colLogsSums.get(index) : 0d;
      colLogsSums.put(index, currSum - xlogx(value));
    }

    Map<Integer, Double> ret = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : rowLogsSums.entrySet()) {
      int index = entry.getKey();
      double logRowSum = entry.getValue();
      double logColumnSum = colLogsSums.get(index);
      int total = sums.get(index);

      double columnEntropy = (xlogx(total) + logColumnSum);
      double logTotal = elemLogsSums.get(index);
      double rowEntropy = (logRowSum + logTotal);
      double ig = 0d;
      if (Math.abs(total) > 1e-6)
        ig = (columnEntropy + rowEntropy) / (total * log2);
      ret.put(index, ig);
    }
    return ret;
  }
}