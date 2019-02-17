/*
 * Copyright 2018 The Apache Software Foundation.
 *
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
 */
package org.apache.samoa.preprocessing.featureselection.ranking.selectors;

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

import com.github.javacliparser.FloatOption;
import com.google.common.collect.Collections2;
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;

import java.util.*;

/**
 * Selects features which are more relevant than configurable static threshold value
 */
public class ThresholdFeatureSelector implements FeatureSelector {

  public FloatOption numericThresholdOption = new FloatOption("numericThreshold",
      'u', "Threshold for numeric attributes",
      0.05, 0.0, Double.MAX_VALUE);

  public FloatOption nominalThresholdOption = new FloatOption("nominalThreshold",
      'o', "Threshold for nominal attributes",
      0.05, 0.0, Double.MAX_VALUE);

  public List<Selection> selectFeatures(InstanceRanking instanceRanking) throws IllegalAccessException {
    if (instanceRanking.getNominalRankingValues().size() + instanceRanking.getNumericRankingValues().size() == 0)
      throw new IllegalAccessException("Ranking does not contain any attributes.");

    double nominalThreshold = nominalThresholdOption.getValue();
    double numericThreshold = numericThresholdOption.getValue();
    Set<Map.Entry<Integer, Double>> numericEntrySet = instanceRanking.getNumericRankingValues().entrySet();
    Set<Map.Entry<Integer, Double>> nominalEntrySet = instanceRanking.getNominalRankingValues().entrySet();
    Utils.GetKeyFromEntryFunction mapFunction = new Utils.GetKeyFromEntryFunction();

    Collection<Integer> selectedNumericIndexes = new LinkedList<>();
    Collection<Integer> selectedNominalIndexes = new LinkedList<>();
    if (numericEntrySet.size() > 0) {
      Utils.RankigAbsThresholdPredicate numericPredicate = new Utils.RankigAbsThresholdPredicate(numericThreshold);
      selectedNumericIndexes = Collections2.transform(
          Collections2.filter(numericEntrySet, numericPredicate), mapFunction);
    }
    if (nominalEntrySet.size() > 0) {
      Utils.RankigAbsThresholdPredicate nominalPredicate = new Utils.RankigAbsThresholdPredicate(nominalThreshold);
      selectedNominalIndexes = Collections2.transform(
          Collections2.filter(nominalEntrySet, nominalPredicate), mapFunction);
    }

    // single selection is returned - other implementations could generate more propositions
    return Collections.singletonList(
        new Selection(CollectionUtils.union(selectedNumericIndexes, selectedNominalIndexes)));
  }
}
