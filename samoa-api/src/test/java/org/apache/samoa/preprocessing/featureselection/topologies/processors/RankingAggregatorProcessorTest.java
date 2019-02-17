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
import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.InstanceContentEvent;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.ranking.selectors.FeatureSelector;
import org.apache.samoa.preprocessing.featureselection.topologies.common.deciders.SelectionDecider;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.preprocessing.featureselection.topologies.events.FeatureSetContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RankingAggregatorProcessorTest {
  @Mocked
  private Stream instanceStream;
  @Mocked
  private Stream rankerStream;
  @Mocked
  private Stream outputStream;

  @Mocked
  private SelectionDecider decider;
  @Mocked
  private FeatureSelector selector;
  @Mocked
  private Splitter splitter;
  @Mocked
  private RankingRepository rankingRepository;

  private final int eventsUntilSelection = 2;

  @Tested
  private RankingAggregatorProcessor processor;

  @Before
  public void setUp() {
    processor = new RankingAggregatorProcessor.Builder()
        .decider(decider)
        .selector(selector)
        .splitter(splitter)
        .rankingRepository(rankingRepository)
        .eventsUntilSelection(eventsUntilSelection)
        .build();
    processor.setInstanceStream(instanceStream);
    processor.setRankerStream(rankerStream);
    processor.setOutputStream(outputStream);
  }

  @Test
  public void testThatProcessInstanceEventWorks(@Mocked final InstanceContentEvent instanceEvent,
      @Mocked final Instance instance,
      @Mocked final FeatureSetContentEvent featureSetEvent) {
    new Expectations() {
      {
        instanceEvent.getInstance();
        result = instance;
        splitter.split(instance);
        result = Arrays.asList(featureSetEvent, featureSetEvent);
      }
    };

    processor.process(instanceEvent);

    new FullVerifications() {
      {
        instanceStream.put(instanceEvent);
        times = 1;
        splitter.split(instance);
        times = 1;
        rankerStream.put(featureSetEvent);
        times = 2;
      }
    };
  }

  @Test
  public void testThatProcessRankingEventWithoutSelectionProcessWorks(@Mocked final RankingContentEvent event)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 1;
        rankingRepository.getEventsProcessed(1);
        result = eventsUntilSelection - 1;
      }
    };

    processor.process(event);

    new FullVerifications() {
      {
        rankingRepository.aggregateEvent(event);
        times = 1;
        event.getRankingIdentifier();
        times = 1;
        rankingRepository.getEventsProcessed(1);
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessRankingEventWithSelectionProcessWorks(@Mocked final RankingContentEvent event,
      @Mocked final InstanceRanking instanceRanking,
      @Mocked final List<Selection> selections)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 1;
        rankingRepository.getEventsProcessed(1);
        result = eventsUntilSelection;
        rankingRepository.getInstanceRanking(1);
        result = instanceRanking;
        selector.selectFeatures(withInstanceOf(InstanceRanking.class));
        result = selections;
      }
    };

    processor.process(event);

    new Verifications() {
      {
        rankingRepository.aggregateEvent(event);
        times = 1;
        event.getRankingIdentifier();
        times = 1;
        rankingRepository.getEventsProcessed(1);
        times = 1;
        rankingRepository.getInstanceRanking(1);
        times = 1;
        selector.selectFeatures(instanceRanking);
        times = 1;
        decider.processSelections(selections, instanceRanking);
        times = 1;
      }
    };
  }
}
