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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.junit.Test;

public class RankingRepositoryTest {
  @Mocked
  private RankingContentEvent event;
  @Tested
  private InstanceRankingAggregator aggregator;

  @Test(expected = IllegalAccessException.class)
  public void testThatGetInstanceRankingWhenRankingDoesNotExistRaisesException() throws IllegalAccessException {
    RankingRepository repository = new RankingRepository();

    repository.getInstanceRanking(0);
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatGetEventsProcessedWhenRankingDoesNotExistRaisesException() throws IllegalAccessException {
    RankingRepository repository = new RankingRepository();

    repository.getEventsProcessed(0);
  }

  @Test
  public void testThatAggregateEventForSingleAggregatorWorks() throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 0;
      }
    };
    RankingRepository repository = new RankingRepository();

    repository.aggregateEvent(event);
    repository.getInstanceRanking(0);

    new Verifications() {
      {
        aggregator.processEvent(event);
        times = 1;
        aggregator.getInstanceRanking();
        times = 1;
      }
    };
  }

  @Test
  public void testThatAggregateEventForMultipleAggregatorsWorks() throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        returns(0, 1);
      }
    };
    RankingRepository repository = new RankingRepository();

    repository.aggregateEvent(event);
    repository.aggregateEvent(event);
    repository.getInstanceRanking(0);
    repository.getInstanceRanking(1);

    new Verifications() {
      {
        aggregator.processEvent(event);
        times = 2;
        aggregator.getInstanceRanking();
        times = 2;
      }
    };
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatPurgeWorks() throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 0;
      }
    };
    RankingRepository repository = new RankingRepository();

    repository.aggregateEvent(event);
    repository.purge();
    repository.getInstanceRanking(0);
  }
}
