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

import com.google.common.collect.Collections2;
import mockit.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.preprocessing.featureselection.ranking.selectors.Utils;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureType;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.ValueIndex;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InformationGainIncrementalRankerTest {

  @Tested
  private InformationGainIncrementalRanker ranker;
  @Mocked
  private FeatureSet featureSet;

  @Before
  public void setUp() {
    ranker = new InformationGainIncrementalRanker();
  }

  @Test(expected = IllegalStateException.class)
  public void testThatProcessWithInvalidFeatureTypeSetRaisesException() {
    new StrictExpectations() {
      {
        featureSet.getType();
        result = FeatureType.Numeric;
      }
    };

    ranker.process(featureSet);
  }

  @Test(expected = IllegalStateException.class)
  public void testThatGetRankingWhenValuesAreNotUpdatedRaisesException() {
    ranker.getRanking().getRankingValues(); // no update
  }

  @Test
  public void testThatGetRankingWhenThereWasNoProcessingWorks() {
    ranker.update();
    Ranking ranking = ranker.getRanking();

    assertEquals(0, ranking.getRankingValues().size());
  }

  @Test
  public void testThatProcessForNormalUseCaseWorks() {
    new Expectations() {
      {
        featureSet.getType();
        result = FeatureType.Nominal;

        featureSet.getValueIndexes();
        returns(
            Arrays.asList(
                new ValueIndex(1, 1),
                new ValueIndex(2, 1)),
            Arrays.asList(
                new ValueIndex(2, 2),
                new ValueIndex(1, Double.NaN)),
            Arrays.asList(
                new ValueIndex(2, Double.NaN),
                new ValueIndex(1, 2)),
            Arrays.asList(
                new ValueIndex(1, 1),
                new ValueIndex(2, 1)),
            Arrays.asList(
                new ValueIndex(1, 2),
                new ValueIndex(2, 1)));

        featureSet.getLabelValue();
        returns(0d, 1d, 1d, 0d, 0d);
      }
    };

    ranker.process(featureSet);
    ranker.process(featureSet);
    ranker.process(featureSet);
    ranker.process(featureSet);
    ranker.process(featureSet);

    assertFalse(ranker.isUpdated());
    ranker.update();
    assertTrue(ranker.isUpdated());
    Ranking ranking = ranker.getRanking();
    Map<Integer, Double> values = ranking.getRankingValues();

    assertEquals(2, values.size());
    assertTrue(
        CollectionUtils.isEqualCollection(
            Arrays.asList(1, 2),
            Collections2.transform(values.entrySet(), new Utils.GetKeyFromEntryFunction())));
    for (Map.Entry<Integer, Double> vi : values.entrySet()) {
      if (vi.getKey() == 1)
        assertEquals(0.44907940649440836, vi.getValue(), 1e-16);
      else
        assertEquals(1.17042692693889, vi.getValue(), 1e-14);
    }

    new Verifications() {
      {
        featureSet.getType();
        times = 5;
        featureSet.getLabelValue();
        times = 5;
        featureSet.getValueIndexes();
        times = 5;
      }
    };
  }

  @Test
  public void testThatProcessWithResettingWorks() {
    new Expectations() {
      {
        featureSet.getType();
        result = FeatureType.Nominal;

        featureSet.getValueIndexes();
        returns(
            Arrays.asList(
                new ValueIndex(1, 3),
                new ValueIndex(2, 4)),
            Arrays.asList(
                new ValueIndex(1, 1),
                new ValueIndex(2, 1)),
            Arrays.asList(
                new ValueIndex(2, 2),
                new ValueIndex(1, 2)),
            Arrays.asList(
                new ValueIndex(1, 1),
                new ValueIndex(2, 1)),
            Arrays.asList(
                new ValueIndex(1, 2),
                new ValueIndex(2, 1)));

        featureSet.getLabelValue();
        returns(1d, 0d, 1d, 0d, 0d);
      }
    };

    ranker.process(featureSet);
    ranker.reset();
    ranker.process(featureSet);
    ranker.process(featureSet);
    ranker.process(featureSet);
    ranker.process(featureSet);
    ranker.update();
    Ranking ranking = ranker.getRanking();
    Map<Integer, Double> values = ranking.getRankingValues();

    assertTrue(ranker.isUpdated());
    assertEquals(2, values.size());
    assertTrue(
        CollectionUtils.isEqualCollection(
            Arrays.asList(1, 2),
            Collections2.transform(values.entrySet(), new Utils.GetKeyFromEntryFunction())));
    for (Map.Entry<Integer, Double> vi : values.entrySet()) {
      if (vi.getKey() == 1)
        assertEquals(0.44907940649440836, vi.getValue(), 1e-17);
      else
        assertEquals(1.17042692693889, vi.getValue(), 1e-14);
    }

    new Verifications() {
      {
        featureSet.getType();
        times = 5;
        featureSet.getLabelValue();
        times = 5;
        featureSet.getValueIndexes();
        times = 5;
      }
    };
  }
}