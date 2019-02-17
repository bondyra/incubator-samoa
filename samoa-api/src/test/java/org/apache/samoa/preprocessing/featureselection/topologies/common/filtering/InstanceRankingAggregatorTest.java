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
import mockit.Mocked;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class InstanceRankingAggregatorTest {
  @Mocked
  private RankingContentEvent event;

  @Test
  public void testThatSingleProcessEventForNormalUseCaseWorks() throws IllegalAccessException {
    new Expectations() {
      {
        event.getNominalRanking();
        result = new Ranking(ImmutableMap.of(0, 1d, 1, 0.5d));
        event.getNumericRanking();
        result = new Ranking(ImmutableMap.of(2, 0.88d, 3, 0.33d));
      }
    };
    InstanceRankingAggregator aggregator = new InstanceRankingAggregator();

    aggregator.processEvent(event);

    InstanceRanking ranking = aggregator.getInstanceRanking();

    assertEquals(1, aggregator.getEventsProcessed());
    assertEquals(2, ranking.getNominalRankingValues().size());
    for (Map.Entry<Integer, Double> entry : ranking.getNominalRankingValues().entrySet()) {
      if (entry.getKey() == 0) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(1), entry.getKey());
        assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    assertEquals(2, ranking.getNumericRankingValues().size());
    for (Map.Entry<Integer, Double> entry : ranking.getNumericRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(0.88d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(0.33d, entry.getValue(), Double.MIN_VALUE);
      }
    }
  }

  @Test
  public void testThatMultipleProcessEventForNormalUseCaseWorks() throws IllegalAccessException {
    new Expectations() {
      {
        event.getNominalRanking();
        returns(
            new Ranking(ImmutableMap.of(0, 1d, 1, 2d)),
            new Ranking(ImmutableMap.of(2, 2d, 3, 1d)));
        event.getNumericRanking();
        returns(
            new Ranking(ImmutableMap.of(4, 1d, 5, 2d)),
            new Ranking(ImmutableMap.of(6, 2d, 7, 1d)));
      }
    };
    InstanceRankingAggregator aggregator = new InstanceRankingAggregator();

    aggregator.processEvent(event);
    aggregator.processEvent(event);

    InstanceRanking ranking = aggregator.getInstanceRanking();

    assertEquals(2, aggregator.getEventsProcessed());
    assertEquals(2, ranking.getNominalRankingValues().size());
    for (Map.Entry<Integer, Double> entry : ranking.getNominalRankingValues().entrySet()) {
      if (entry.getKey() == 2) {
        assertEquals(2d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(3), entry.getKey());
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    assertEquals(2, ranking.getNumericRankingValues().size());
    for (Map.Entry<Integer, Double> entry : ranking.getNumericRankingValues().entrySet()) {
      if (entry.getKey() == 6) {
        assertEquals(2d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(7), entry.getKey());
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      }
    }
  }
}
