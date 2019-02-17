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

import com.google.common.collect.ImmutableMap;
import mockit.Tested;
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThresholdFeatureSelectorTest {
  @Tested
  private ThresholdFeatureSelector selector;

  @Before
  public void setUp() {
    selector = new ThresholdFeatureSelector();
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatSelectFeaturesWithNoIndexesRaisesException() throws IllegalAccessException {
    selector.nominalThresholdOption.setValue(0.7);
    selector.numericThresholdOption.setValue(0.7);
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(new HashMap<Integer, Double>()));
    ranking.updateNumericRanking(new Ranking(new HashMap<Integer, Double>()));

    List<Selection> selections = selector.selectFeatures(ranking);
  }

  @Test
  public void testThatSelectFeaturesWithNominalAttributesOnlyWorks() throws IllegalAccessException {
    selector.nominalThresholdOption.setValue(0.7);
    selector.numericThresholdOption.setValue(0.7);
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(
        ImmutableMap.of(
            0, 1d,
            1, 0.71d,
            2, 0.69d)));
    ranking.updateNumericRanking(new Ranking(new HashMap<Integer, Double>()));

    List<Selection> selections = selector.selectFeatures(ranking);
    assertEquals(selections.size(), 1);
    assertTrue(CollectionUtils.isEqualCollection(selections.get(0).getSelectedIndexes(), Arrays.asList(0, 1)));
  }

  @Test
  public void testThatSelectFeaturesWithNumericAttributesOnlyWorks() throws IllegalAccessException {
    selector.nominalThresholdOption.setValue(0.7);
    selector.numericThresholdOption.setValue(0.7);
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(new HashMap<Integer, Double>()));
    ranking.updateNumericRanking(new Ranking(
        ImmutableMap.of(
            3, 1d,
            4, 0.71d,
            5, 0.69d)));

    List<Selection> selections = selector.selectFeatures(ranking);
    assertEquals(selections.size(), 1);
    assertTrue(CollectionUtils.isEqualCollection(selections.get(0).getSelectedIndexes(), Arrays.asList(3, 4)));
  }

  @Test
  public void testThatSelectFeaturesWithFullRankingWorks() throws IllegalAccessException {
    selector.nominalThresholdOption.setValue(0.7);
    selector.numericThresholdOption.setValue(0.7);
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(
        ImmutableMap.of(
            0, 0.69d,
            2, 0.71d,
            1, 1d,
            3, 0.01d)));
    ranking.updateNumericRanking(new Ranking(
        ImmutableMap.of(
            7, 0.01d,
            4, 1d,
            6, 0.69d,
            5, 0.71d)));

    List<Selection> selections = selector.selectFeatures(ranking);
    assertEquals(selections.size(), 1);
    assertTrue(CollectionUtils.isEqualCollection(selections.get(0).getSelectedIndexes(), Arrays.asList(1, 2, 4, 5)));
  }
}
