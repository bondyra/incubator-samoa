package org.apache.samoa.preprocessing.featureselection.topologies.processors;

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
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.events.DriftContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.FeatureSetContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.WarningContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

public class RankerNodeProcessorTest {
  @Mocked
  private RankingWorkerRepository rankingWorkerRepository;
  @Mocked
  private Stream stream;

  @Tested
  private RankerNodeProcessor processor;

  @Before
  public void setUp() {
    processor = new RankerNodeProcessor.Builder()
        .rankingWorkerRepository(rankingWorkerRepository)
        .build();
    processor.setOutputStream(stream);
  }

  @Test
  public void testThatProcessFeatureSetsWithNotPreparedRankingEventWorks(@Mocked final FeatureSet numericSet,
      @Mocked final FeatureSet nominalSet,
      @Mocked final FeatureSetContentEvent event)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 0;
        event.getNumericSet();
        result = numericSet;
        event.getNominalSet();
        result = nominalSet;
        rankingWorkerRepository.processFeatureSets(0, numericSet, nominalSet);
        result = false;
      }
    };

    processor.process(event);

    new FullVerifications() {
      {
        event.getRankingIdentifier();
        times = 1;
        event.getNumericSet();
        times = 1;
        event.getNominalSet();
        times = 1;
        rankingWorkerRepository.processFeatureSets(0, numericSet, nominalSet);
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessFeatureSetsWithRankingEventPreparedWorks(@Mocked final FeatureSet numericSet,
      @Mocked final FeatureSet nominalSet,
      @Mocked final FeatureSetContentEvent event,
      @Mocked final RankingContentEvent rankingEvent)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        returns(0, 1);
        event.getNumericSet();
        result = numericSet;
        event.getNominalSet();
        result = nominalSet;
        rankingWorkerRepository.processFeatureSets(0, numericSet, nominalSet);
        result = false;
        rankingWorkerRepository.processFeatureSets(1, numericSet, nominalSet);
        result = true;
        rankingWorkerRepository.getEvent(1);
        result = rankingEvent;
      }
    };

    processor.process(event);
    processor.process(event);

    new FullVerifications() {
      {
        event.getRankingIdentifier();
        times = 2;
        event.getNumericSet();
        times = 2;
        event.getNominalSet();
        times = 2;
        rankingWorkerRepository.processFeatureSets(0, numericSet, nominalSet);
        times = 1;
        rankingWorkerRepository.processFeatureSets(1, numericSet, nominalSet);
        times = 1;
        rankingWorkerRepository.getEvent(1);
        times = 1;
        stream.put(rankingEvent);
        times = 1;
      }
    };
  }

  @Test
  public void testThatDriftSignalIsHandled(
      @Mocked final DriftContentEvent event) {

    processor.process(event);

    new FullVerifications() {
      {
        rankingWorkerRepository.processDrift();
        times = 1;
      }
    };
  }

  @Test
  public void testThatWarningSignalIsHandled(
      @Mocked final WarningContentEvent event) {

    processor.process(event);

    new FullVerifications() {
      {
        rankingWorkerRepository.processWarning();
        times = 1;
      }
    };
  }
}
