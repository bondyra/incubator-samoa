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
 * Incremental ranker that uses symmetrical uncertainty measure
 */
public class SymmetricalUncertaintyIncrementalRanker extends InformationTheoryIncrementalRanker {
  @Override
  public void update() {
    if (updated)
      return;
    this.rankingValues = this.getSymmetricalUncertainty();
    updated = true;
  }

  /**
   * Compute symmetrical uncertainty for all known features
   * 
   * @return Map of feature index and features' current symmetrical uncertainty value
   */
  private Map<Integer, Double> getSymmetricalUncertainty() {
    HashMap<ComputeKey, Integer> colSumMap = new HashMap<>();
    HashMap<ComputeKey, Integer> rowSumMap = new HashMap<>();
    HashMap<Integer, Double> elemLogsMap = new HashMap<>();
    HashMap<Integer, Double> colLogsMap = new HashMap<>();
    HashMap<Integer, Double> rowLogsMap = new HashMap<>();
    HashMap<Integer, Double> elemSumMap = new HashMap<>();

    for (Map.Entry<Key, Integer> entry : this.contingencyTable.entrySet()) {
      int index = entry.getKey().featureIndex;
      double classValue = entry.getKey().classValue;
      double featureValue = entry.getKey().featureValue;
      ComputeKey columnKey = new ComputeKey(index, classValue);
      ComputeKey rowKey = new ComputeKey(index, featureValue);
      int entryValue = entry.getValue();
      int currentSumColumn = colSumMap.containsKey(columnKey) ? colSumMap.get(columnKey) : 0;
      int currentSumRow = rowSumMap.containsKey(rowKey) ? rowSumMap.get(rowKey) : 0;
      colSumMap.put(columnKey, currentSumColumn + entryValue);
      rowSumMap.put(rowKey, currentSumRow + entryValue);
      double currLogTotal = elemLogsMap.containsKey(index) ? elemLogsMap.get(index) : 0d;
      elemLogsMap.put(index, currLogTotal + xlogx(entryValue));
    }

    for (Map.Entry<ComputeKey, Integer> entry : rowSumMap.entrySet()) {
      int index = entry.getKey().featureIndex;
      int value = entry.getValue();
      double currSum = rowLogsMap.containsKey(index) ? rowLogsMap.get(index) : 0d;
      rowLogsMap.put(index, currSum + xlogx(value));
    }
    for (Map.Entry<ComputeKey, Integer> entry : colSumMap.entrySet()) {
      int index = entry.getKey().featureIndex;
      int value = entry.getValue();
      double currSum = colLogsMap.containsKey(index) ? colLogsMap.get(index) : 0d;
      colLogsMap.put(index, currSum + xlogx(value));
      double currTotal = elemSumMap.containsKey(index) ? elemSumMap.get(index) : 0d;
      elemSumMap.put(index, currTotal + value);
    }

    Map<Integer, Double> ret = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : rowLogsMap.entrySet()) {
      int index = entry.getKey();
      double logRowSum = entry.getValue();
      double logColSum = colLogsMap.get(index);
      double elemLogsSum = elemLogsMap.get(index);
      double logElemSum = xlogx(elemSumMap.get(index));
      double su = 0;

      if (Math.abs(logColSum + logRowSum) > 1e-6)
        su = 2.0 * ((logColSum + logRowSum - elemLogsSum - logElemSum) / (logColSum + logRowSum - 2 * logElemSum));
      ret.put(index, su);
    }
    return ret;
  }
}