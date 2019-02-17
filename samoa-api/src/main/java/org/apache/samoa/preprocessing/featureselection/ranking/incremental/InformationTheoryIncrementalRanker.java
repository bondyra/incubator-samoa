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

import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureType;
import org.apache.samoa.preprocessing.featureselection.ranking.models.ValueIndex;

import java.io.Serializable;
import java.util.*;

/**
 * Abstract incremental ranker that uses infromation theory measures
 */
public abstract class InformationTheoryIncrementalRanker implements NominalIncrementalRanker, Serializable {
  static final long serialVersionUID = -1949849512589218930L;

  protected Map<Key, Integer> contingencyTable = new HashMap<>();
  protected Map<Integer, Double> rankingValues;
  protected boolean updated = false;

  @Override
  public void process(FeatureSet set) throws IllegalStateException {
    if (set.getType() != FeatureType.Nominal) // designated to nominal attributes only
      throw new IllegalStateException();
    Iterable<ValueIndex> valueIndices = set.getValueIndexes();
    double classValue = set.getLabelValue();
    for (ValueIndex valueIndex : valueIndices) {
      if (Double.isNaN(valueIndex.getValue()))
        continue; // do not update missing value, no point in that
      Key key = new Key(valueIndex.getIndex(), valueIndex.getValue(), classValue);
      int count = contingencyTable.containsKey(key) ? contingencyTable.get(key) : 0;
      contingencyTable.put(key, count + 1);
    }
    updated = false;
  }

  @Override
  public boolean isUpdated() {
    return updated;
  }

  @Override
  public void reset() {
    contingencyTable.clear();
    rankingValues = null;
    updated = false;
  }

  @Override
  public Ranking getRanking() {
    return new Ranking(rankingValues);
  }

  // caching of smaller log values could be considered to boost performance (similarly to WEKA implementation)
  protected static double log2 = Math.log(2);

  protected double xlogx(double val) {
    if (val < 1e-6) {
      return 0;
    }
    return val * Math.log(val) / log2;
  }

  /**
   * Compute entropies for all known features
   * 
   * @return Map of feature index and features' current entropy value
   */
  protected Map<Integer, Double> getEntropy() {
    HashMap<ComputeKey, Integer> sumColumns = new HashMap<>();
    HashMap<Integer, Integer> totalMap = new HashMap<>();
    HashMap<Integer, Double> logColumnSumMap = new HashMap<>();

    for (Map.Entry<Key, Integer> entry : this.contingencyTable.entrySet()) {
      double classValue = entry.getKey().classValue;
      int featureIndex = entry.getKey().featureIndex;
      ComputeKey key = new ComputeKey(featureIndex, classValue);
      int entryValue = entry.getValue();
      int currentSum = sumColumns.containsKey(key) ? sumColumns.get(key) : 0;
      sumColumns.put(key, currentSum + entryValue);
    }
    for (Map.Entry<ComputeKey, Integer> entry : sumColumns.entrySet()) {
      int index = entry.getKey().featureIndex;
      int value = entry.getValue();
      int count = totalMap.containsKey(index) ? totalMap.get(index) : 0;
      count += value;
      totalMap.put(index, count);
      double currSum = logColumnSumMap.containsKey(index) ? logColumnSumMap.get(index) : 0d;
      logColumnSumMap.put(index, currSum + xlogx(value));
    }
    HashMap<Integer, Double> ret = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : logColumnSumMap.entrySet()) {
      int index = entry.getKey();
      double logColumnSum = entry.getValue();
      int total = totalMap.get(index);
      double entropy = 0;
      if (Math.abs(total) > 1e-6)
        entropy = (xlogx(total) - logColumnSum) / (total * Math.log(2));
      ret.put(index, entropy);
    }
    return ret;
  }

  /**
   * Compute conditional entropy for all known features
   * 
   * @return Map of feature index and features' current conditional entropy value
   */
  protected Map<Integer, Double> getConditionalEntropy() {
    HashMap<ComputeKey, Integer> sumRows = new HashMap<>();
    HashMap<Integer, Integer> totalMap = new HashMap<>();
    HashMap<Integer, Double> logTotalMap = new HashMap<>();
    HashMap<Integer, Double> logRowSumMap = new HashMap<>();

    for (Map.Entry<Key, Integer> entry : this.contingencyTable.entrySet()) {
      int index = entry.getKey().featureIndex;
      double featureValue = entry.getKey().featureValue;
      ComputeKey key = new ComputeKey(index, featureValue);
      int entryValue = entry.getValue();
      int currentSum = sumRows.containsKey(key) ? sumRows.get(key) : 0;
      sumRows.put(key, currentSum + entryValue);
      double currLogTotal = logTotalMap.containsKey(index) ? logTotalMap.get(index) : 0d;
      logTotalMap.put(index, currLogTotal + xlogx(entryValue));
    }
    for (Map.Entry<ComputeKey, Integer> entry : sumRows.entrySet()) {
      int index = entry.getKey().featureIndex;
      int value = entry.getValue();
      int count = totalMap.containsKey(index) ? totalMap.get(index) : 0;
      count += value;
      totalMap.put(index, count);
      double currSum = logRowSumMap.containsKey(index) ? logRowSumMap.get(index) : 0d;
      logRowSumMap.put(index, currSum + xlogx(value));
    }
    HashMap<Integer, Double> ret = new HashMap<>();
    for (Map.Entry<Integer, Double> entry : logRowSumMap.entrySet()) {
      int index = entry.getKey();
      double logRowSum = entry.getValue();
      int total = totalMap.get(index);
      double logTotal = logTotalMap.get(index);
      double condEntropy = 0;
      if (Math.abs(total) > 1e-6)
        condEntropy = (logRowSum - logTotal) / (total * Math.log(2));
      ret.put(index, condEntropy);
    }
    return ret;
  }

  /**
   * Key used in contingency tables. Consists of feature index, feature value and class value
   */
  protected class Key {
    public int featureIndex;
    public double featureValue;
    public double classValue;

    public Key(int featureIndex, double featureValue, double classValue) {
      this.featureValue = featureValue;
      this.featureIndex = featureIndex;
      this.classValue = classValue;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Key other = (Key) obj;
      return other.classValue == classValue
          && other.featureIndex == featureIndex
          && other.featureValue == featureValue;
    }

    @Override
    public int hashCode() {
      return Float.floatToIntBits((float) featureValue)
          + 15 * Float.floatToIntBits((float) classValue)
          + 31 * featureIndex;
    }

    @Override
    public String toString() {
      return featureIndex + ":" + featureValue + "(" + classValue + ")";
    }
  }

  /**
   * Smaller key used in internal entropy computations. Consists of feature index and feature value
   */
  protected class ComputeKey {
    public int featureIndex;
    public double value;

    public ComputeKey(int featureIndex, double value) {
      this.featureIndex = featureIndex;
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ComputeKey other = (ComputeKey) obj;
      return other.featureIndex == featureIndex
          && other.value == value;
    }

    @Override
    public int hashCode() {
      return Float.floatToIntBits((float) value)
          + 31 * featureIndex;
    }
  }
}
