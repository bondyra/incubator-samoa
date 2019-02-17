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

import com.google.common.collect.ImmutableMap;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class BatchRankingWorkerTest {
  @Injectable
  private IncrementalRanker numericRanker;
  @Injectable
  private IncrementalRanker nominalRanker;

  @Mocked
  private FeatureSet numericFeatureSet;
  @Mocked
  private FeatureSet nominalFeatureSet;

  @Test(expected = IllegalStateException.class)
  public void testThatProcessWhenDifferentInstanceNumbersAreDeliveredRaisesException() throws IllegalStateException {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalFeatureSet.getInstanceNumber();
        returns(1L);
        numericFeatureSet.getInstanceNumber();
        returns(2L);
      }
    };

    worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
  }

  @Test(expected = IllegalStateException.class)
  public void testThatProcessWhenNotPositiveInstanceNumberIsDeliveredRaisesException() throws IllegalStateException {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalFeatureSet.getInstanceNumber();
        returns(0L);
        numericFeatureSet.getInstanceNumber();
        returns(0L);
      }
    };

    worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
  }

  @Test(expected = IllegalStateException.class)
  public void testThatGetEventWhenEventIsNotPreparedRaisesException() throws IllegalStateException {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalFeatureSet.getInstanceNumber();
        returns(1L, 2L);
        numericFeatureSet.getInstanceNumber();
        returns(1L, 2L);
      }
    };

    worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event = worker.getEvent();
  }

  @Test
  public void testThatGetEventAfterResettingWorks() throws IllegalStateException {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(0, 1d, 1, 0.5d)));
        numericRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(2, 1d, 3, 0.5d)));

        nominalFeatureSet.getInstanceNumber();
        returns(1L, 2L, 3L, 4L);
        numericFeatureSet.getInstanceNumber();
        returns(1L, 2L, 3L, 4L);
      }
    };

    boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    worker.reset();
    boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result4 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event = worker.getEvent();

    assertFalse(result1);
    assertFalse(result2);
    assertFalse(result3);
    assertTrue(result4);
    assertEquals(0, event.getRankingIdentifier());
    for (Map.Entry<Integer, Double> entry : event.getNominalRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    for (Map.Entry<Integer, Double> entry : event.getNumericRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }

    new Verifications() {
      {
        nominalRanker.getRanking();
        times = 1;
        numericRanker.getRanking();
        times = 1;

        nominalRanker.process(nominalFeatureSet);
        times = 4;
        numericRanker.process(numericFeatureSet);
        times = 4;

        nominalRanker.reset();
        times = 2;
        numericRanker.reset();
        times = 2;
      }
    };
  }

  @Test
  public void testThatGetEventAfterResettingAndInstanceDeliveredAgainWorks() throws IllegalStateException {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(0, 1d, 1, 0.5d)));
        numericRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(2, 1d, 3, 0.5d)));

        nominalFeatureSet.getInstanceNumber();
        returns(1L, 1L, 2L, 3L);
        numericFeatureSet.getInstanceNumber();
        returns(1L, 1L, 2L, 3L);
      }
    };

    boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    worker.reset();
    boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result4 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event = worker.getEvent();

    assertFalse(result1);
    assertFalse(result2);
    assertFalse(result3);
    assertTrue(result4);
    assertEquals(0, event.getRankingIdentifier());
    for (Map.Entry<Integer, Double> entry : event.getNominalRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    for (Map.Entry<Integer, Double> entry : event.getNumericRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }

    new Verifications() {
      {
        nominalRanker.getRanking();
        times = 1;
        numericRanker.getRanking();
        times = 1;

        nominalRanker.process(nominalFeatureSet);
        times = 4;
        numericRanker.process(numericFeatureSet);
        times = 4;

        nominalRanker.reset();
        times = 2;
        numericRanker.reset();
        times = 2;
      }
    };
  }

  @Test
  public void testThatProcessWhenThereIsSingleFeatureSetPerInstanceWorks() {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(0, 1d, 1, 0.5d)),
            new Ranking(ImmutableMap.of(0, 0.5d, 1, 1d)));
        numericRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(2, 1d, 3, 0.5d)),
            new Ranking(ImmutableMap.of(2, 0.5d, 3, 1d)));

        nominalFeatureSet.getInstanceNumber();
        returns(1L, 2L, 3L, 3L, 4L, 5L);
        numericFeatureSet.getInstanceNumber();
        returns(1L, 2L, 3L, 3L, 4L, 5L);
      }
    };

    boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event1 = worker.getEvent();
    boolean result4 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result5 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event1b = worker.getEvent();
    boolean result6 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event2 = worker.getEvent();

    assertFalse(result1);
    assertFalse(result2);
    assertTrue(result3);
    assertFalse(result4);
    assertFalse(result5);
    assertTrue(result6);
    assertSame(event1, event1b);
    assertEquals(0, event1.getRankingIdentifier());
    for (Map.Entry<Integer, Double> entry : event1.getNominalRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    for (Map.Entry<Integer, Double> entry : event1.getNumericRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    assertEquals(0, event2.getRankingIdentifier());
    for (Map.Entry<Integer, Double> entry : event2.getNominalRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    for (Map.Entry<Integer, Double> entry : event2.getNumericRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      }
    }

    new Verifications() {
      {
        nominalRanker.getRanking();
        times = 2;
        numericRanker.getRanking();
        times = 2;

        nominalRanker.process(nominalFeatureSet);
        times = 6;
        numericRanker.process(numericFeatureSet);
        times = 6;

        nominalRanker.reset();
        times = 2;
        numericRanker.reset();
        times = 2;
      }
    };
  }

  @Test
  public void testThatProcessWhenThereAreMultipleFeatureSetsPerInstanceWorks() {
    BatchRankingWorker worker = new BatchRankingWorker(0, 2, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(0, 1d, 1, 0.5d)));
        numericRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(2, 1d, 3, 0.5d)));

        nominalFeatureSet.getInstanceNumber();
        returns(1L, 1L, 2L, 3L);
        numericFeatureSet.getInstanceNumber();
        returns(1L, 1L, 2L, 3L);
      }
    };

    boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result4 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event = worker.getEvent();

    assertFalse(result1);
    assertFalse(result2);
    assertFalse(result3);
    assertTrue(result4);
    assertEquals(0, event.getRankingIdentifier());
    for (Map.Entry<Integer, Double> entry : event.getNominalRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    for (Map.Entry<Integer, Double> entry : event.getNumericRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }

    new Verifications() {
      {
        nominalRanker.getRanking();
        times = 1;
        numericRanker.getRanking();
        times = 1;

        nominalRanker.process(nominalFeatureSet);
        times = 4;
        numericRanker.process(numericFeatureSet);
        times = 4;

        nominalRanker.reset();
        times = 1;
        numericRanker.reset();
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessWhenInstancesAreOutOfOrderWorks() {
    BatchRankingWorker worker = new BatchRankingWorker(0, 3, numericRanker, nominalRanker);
    new Expectations() {
      {
        nominalRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(0, 1d, 1, 0.5d)));
        numericRanker.getRanking();
        returns(
            new Ranking(ImmutableMap.of(2, 1d, 3, 0.5d)));

        nominalFeatureSet.getInstanceNumber();
        returns(1L, 2L, 1L, 3L, 4L);
        numericFeatureSet.getInstanceNumber();
        returns(1L, 2L, 1L, 3L, 4L);
      }
    };

    boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result4 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    boolean result5 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
    RankingContentEvent event = worker.getEvent();

    assertFalse(result1);
    assertFalse(result2);
    assertFalse(result3);
    assertFalse(result4);
    assertTrue(result5);
    assertEquals(0, event.getRankingIdentifier());
    for (Map.Entry<Integer, Double> entry : event.getNominalRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    for (Map.Entry<Integer, Double> entry : event.getNumericRanking().getRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }

    new Verifications() {
      {
        nominalRanker.getRanking();
        times = 1;
        numericRanker.getRanking();
        times = 1;

        nominalRanker.process(nominalFeatureSet);
        times = 4;
        numericRanker.process(numericFeatureSet);
        times = 4;

        nominalRanker.reset();
        times = 1;
        numericRanker.reset();
        times = 1;
      }
    };
  }
}
