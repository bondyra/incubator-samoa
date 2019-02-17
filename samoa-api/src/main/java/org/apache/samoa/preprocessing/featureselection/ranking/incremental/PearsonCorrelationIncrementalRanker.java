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

import java.util.*;

/**
 * Incremental ranker that uses linear Pearson correlation coefficient
 */
public class PearsonCorrelationIncrementalRanker implements NumericIncrementalRanker {

  private Map<Integer, Double> correlations;

  private Map<Integer, Integer> processedInstances = new HashMap<>();
  private Map<Integer, Double> classValueSquareSums = new HashMap<>();
  private Map<Integer, Double> classValueSums = new HashMap<>();
  private Map<Integer, Double> featureValueSums = new HashMap<>();
  private Map<Integer, Double> featureValueSquaresSums = new HashMap<>();
  private Map<Integer, Double> featureValueClassValueProductSums = new HashMap<>();

  private Collection<Integer> knownIndexes = new HashSet<>();

  private boolean updated = false;

  @Override
  public void process(FeatureSet set) throws IllegalStateException {
    if (set.getType() != FeatureType.Numeric) // designated to numeric attributes only
      throw new IllegalStateException();
    double classValue = set.getLabelValue();
    Iterable<ValueIndex> valueIndices = set.getValueIndexes();
    for (ValueIndex valueIndex : valueIndices) {
      if (Double.isNaN(valueIndex.getValue()))
        continue; // do not update missing value, no point in that
      int index = valueIndex.getIndex();

      int processedForAttr = processedInstances.containsKey(index) ? processedInstances.get(index) : 0;
      processedInstances.put(index, processedForAttr + 1);
      double classSumsForAttr = classValueSums.containsKey(index) ? classValueSums.get(index) : 0d;
      classValueSums.put(index, classSumsForAttr + classValue);
      double classSqSumsForAttr = classValueSquareSums.containsKey(index) ? classValueSquareSums.get(index) : 0d;
      classValueSquareSums.put(index, classSqSumsForAttr + (classValue * classValue));

      double value = valueIndex.getValue();
      knownIndexes.add(index); // adds or checks if index is in set
      double val = featureValueSums.containsKey(index) ? featureValueSums.get(index) : 0d;
      featureValueSums.put(index, val + value);
      val = featureValueSquaresSums.containsKey(index) ? featureValueSquaresSums.get(index) : 0d;
      featureValueSquaresSums.put(index, val + value * value);
      val = featureValueClassValueProductSums.containsKey(index) ? featureValueClassValueProductSums.get(index) : 0d;
      featureValueClassValueProductSums.put(index, val + value * classValue);
    }
    updated = false;
  }

  @Override
  public void update() {
    if (updated)
      return;

    correlations = this.getCorrelations();

    updated = true;
  }

  @Override
  public boolean isUpdated() {
    return updated;
  }

  @Override
  public void reset() {
    featureValueSums.clear();
    ;
    featureValueClassValueProductSums.clear();
    featureValueSquaresSums.clear();
    classValueSums.clear();
    classValueSquareSums.clear();
    knownIndexes.clear();
    processedInstances.clear();
    correlations = null;
    updated = false;
  }

  @Override
  public Ranking getRanking() {
    return new Ranking(correlations);
  }

  private Map<Integer, Double> getCorrelations() {
    Map<Integer, Double> corrs = new HashMap<>();
    for (int index : knownIndexes) {
      int processedInstancesForAttr = processedInstances.get(index);

      double ey = classValueSums.get(index) / processedInstancesForAttr;
      double ey2 = classValueSquareSums.get(index) / processedInstancesForAttr;

      double ex = featureValueSums.get(index) / processedInstancesForAttr;
      double ex2 = featureValueSquaresSums.get(index) / processedInstancesForAttr;
      double exy = featureValueClassValueProductSums.get(index) / processedInstancesForAttr;

      double correlation = (exy - ex * ey) / (Math.sqrt(ex2 - ex * ex) * Math.sqrt(ey2 - ey * ey));
      corrs.put(index, correlation);
    }
    return corrs;
  }
}
