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

import mockit.*;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RankingWorkerRepositoryTest {
  @Mocked
  private RankingWorker worker1;
  @Mocked
  private RankingWorker worker2;

  @Tested
  private RankingWorkerRepository repository;

  @Before
  public void setUp() {
    Map<Integer, RankingWorker> workers = new HashMap<>();
    workers.put(1, worker1);
    workers.put(2, worker2);
    repository = new RankingWorkerRepository(workers);
  }

  @Test
  public void testThatDriftSignalIsHandled() {

    repository.processDrift();

    new FullVerifications() {
      {
        worker1.processDrift();
        times = 1;
        worker2.processDrift();
        times = 1;
      }
    };
  }

  @Test
  public void testThatWarningSignalIsHandled() {

    repository.processWarning();

    new FullVerifications() {
      {
        worker1.processWarning();
        times = 1;
        worker2.processWarning();
        times = 1;
      }
    };
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatProcessFeatureSetsWhenRankingIsUnknownRaisesException(@Mocked final FeatureSet numericSet,
      @Mocked final FeatureSet nominalSet)
      throws IllegalAccessException {

    repository.processFeatureSets(3, numericSet, nominalSet);
  }

  @Test
  public void testThatProcessFeatureSetsForNormalUseCaseWorks(@Mocked final FeatureSet numericSet,
      @Mocked final FeatureSet nominalSet)
      throws IllegalAccessException {
    new Expectations() {
      {
        worker1.processFeatureSets(numericSet, nominalSet);
        result = false;
        worker2.processFeatureSets(numericSet, nominalSet);
        result = true;
      }
    };

    boolean result1 = repository.processFeatureSets(1, numericSet, nominalSet);
    boolean result2 = repository.processFeatureSets(2, numericSet, nominalSet);

    assertFalse(result1);
    assertTrue(result2);

    new Verifications() {
      {
        worker1.processFeatureSets(numericSet, nominalSet);
        times = 1;
        worker2.processFeatureSets(numericSet, nominalSet);
        times = 1;
      }
    };
  }

  @Test
  public void testThatGetEventForKnownRankingWorks(@Mocked final RankingContentEvent event)
      throws IllegalAccessException {
    new Expectations() {
      {
        worker1.getEvent();
        result = event;
      }
    };

    RankingContentEvent outputEvent = repository.getEvent(1);

    assertEquals(outputEvent, event);
    new Verifications() {
      {
        worker1.getEvent();
        times = 1;
        worker2.getEvent();
        times = 0;
      }
    };
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatGetEventForUnknownRankingRaisesException() throws IllegalAccessException {

    repository.getEvent(3);
  }
}
