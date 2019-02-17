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
import org.apache.samoa.preprocessing.featureselection.topologies.common.deciders.EvaluationDecider;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.Evaluation;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.preprocessing.featureselection.topologies.events.*;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WrapperAggregatorProcessorTest {
  @Mocked
  private Stream instanceStream;
  @Mocked
  private Stream rankerStream;
  @Mocked
  private Stream evaluatorStream;
  @Mocked
  private Stream outputStream;

  @Mocked
  private EvaluationDecider decider;
  @Mocked
  private EvaluationRepository evaluationRepository;
  @Mocked
  private RankingRepository rankingRepository;
  @Mocked
  private FeatureSelector selector;
  @Mocked
  private Splitter splitter;

  private final int eventsUntilSelection = 2;

  @Tested
  private WrapperAggregatorProcessor processor;

  @Before
  public void setUp() {
    processor = new WrapperAggregatorProcessor.Builder()
        .decider(decider)
        .evaluationRepository(evaluationRepository)
        .rankingRepository(rankingRepository)
        .selectors(Collections.singletonList(selector))
        .splitter(splitter)
        .eventsUntilSelection(eventsUntilSelection)
        .build();

    processor.setInstanceStream(instanceStream);
    processor.setRankerStream(rankerStream);
    processor.setEvaluatorStream(evaluatorStream);
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
  public void testThatDriftSignalIsHandled(@Mocked final DriftContentEvent driftEvent) {
    processor.process(driftEvent);

    new FullVerifications() {
      {
        evaluationRepository.purge();
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessEvaluationResultEventWhenNotPreparedForDecisionYetWorks(
      @Mocked final EvaluationResultContentEvent event)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getEvaluationId();
        result = 1;
        event.getEvaluationResult();
        result = 0.5d;
        event.getEvaluationUpperBound();
        result = 1d;
        evaluationRepository.updateEvaluationResult(1, 0.5d, 1d);
        result = false;
      }
    };

    processor.process(event);

    new FullVerifications() {
      {
        event.getEvaluationId();
        times = 1;
        event.getEvaluationResult();
        times = 1;
        event.getEvaluationUpperBound();
        times = 1;
        evaluationRepository.updateEvaluationResult(1, 0.5d, 1d);
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessEvaluationResultEventWhenPreparedForDecisionWorks(
      @Mocked final EvaluationResultContentEvent event,
      @Mocked final List<Evaluation> evaluationGroup)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getEvaluationId();
        result = 1;
        event.getEvaluationResult();
        result = 0.5d;
        event.getEvaluationUpperBound();
        result = 1d;
        evaluationRepository.updateEvaluationResult(1, 0.5d, 1d);
        result = true;
        evaluationRepository.getGroupByEvaluationId(1);
        result = 1;
        evaluationRepository.getEvaluationGroup(1);
        result = evaluationGroup;
      }
    };

    processor.process(event);

    new FullVerifications() {
      {
        event.getEvaluationId();
        times = 1;
        event.getEvaluationResult();
        times = 1;
        event.getEvaluationUpperBound();
        times = 1;
        evaluationRepository.updateEvaluationResult(1, 0.5d, 1d);
        times = 1;
        evaluationRepository.getGroupByEvaluationId(1);
        times = 1;
        evaluationRepository.getEvaluationGroup(1);
        times = 1;
        decider.processEvaluationResults(evaluationGroup);
        times = 1;
        evaluationRepository.removeEvaluationGroup(1);
        times = 1;
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
  public void testThatProcessRankingEventWithSelectionProcessWithNewSelectionsLeadsToEvaluation(
      @Mocked final RankingContentEvent event,
      @Mocked final InstanceRanking instanceRanking,
      @Mocked final Selection selection,
      @Mocked final Selection bestSelection,
      @Mocked final List<Selection> selections,
      @Mocked final Evaluation evaluationGroupMember)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 1;
        rankingRepository.getEventsProcessed(1);
        result = eventsUntilSelection; // selection will take place
        rankingRepository.getInstanceRanking(1);
        result = instanceRanking;
        selector.selectFeatures(instanceRanking);
        result = Arrays.asList(selection, selection);
        decider.getBestSelection();
        result = bestSelection;
        selection.equalTo(bestSelection);
        returns(true, false);
        evaluationRepository.createEvaluationGroup(withAny(selections));
        result = 1;
        evaluationRepository.getEvaluationGroup(1);
        result = Arrays.asList(evaluationGroupMember, evaluationGroupMember);
        evaluationGroupMember.getEvaluationId();
        returns(0, 1);
        evaluationGroupMember.getSelection();
        result = selection;
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
        rankingRepository.getInstanceRanking(1);
        times = 1;
        selector.selectFeatures(instanceRanking);
        times = 1;
        decider.getBestSelection();
        times = 1;
        selection.equalTo(bestSelection);
        times = 2;
        evaluationRepository.createEvaluationGroup(withAny(selections));
        times = 1;
        evaluationRepository.getEvaluationGroup(1);
        times = 1;
        List<EvaluationContentEvent> events = new ArrayList<>(2);
        evaluatorStream.put(withCapture(events));
        assertEquals(0, events.get(0).getEvaluationId());
        assertEquals(1, events.get(1).getEvaluationId());
        evaluationGroupMember.getEvaluationId();
        times = 2;
        evaluationGroupMember.getSelection();
        times = 2;
      }
    };
  }

  @Test
  public void testThatProcessRankingWithSelectionProcessWithoutNewSelectionDoesNotLeadToEvaluation(
      @Mocked final RankingContentEvent event,
      @Mocked final InstanceRanking instanceRanking,
      @Mocked final Selection selection,
      @Mocked final Selection bestSelection)
      throws IllegalAccessException {
    new Expectations() {
      {
        event.getRankingIdentifier();
        result = 1;
        rankingRepository.getEventsProcessed(1);
        result = eventsUntilSelection; // selection will take place
        rankingRepository.getInstanceRanking(1);
        result = instanceRanking;
        selector.selectFeatures(instanceRanking);
        result = Collections.singletonList(selection);
        decider.getBestSelection();
        result = bestSelection;
        selection.equalTo(bestSelection);
        result = true; // no new selection - so evaluation wont take place
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
        rankingRepository.getInstanceRanking(1);
        times = 1;
        selector.selectFeatures(instanceRanking);
        times = 1;
        decider.getBestSelection();
        times = 1;
        selection.equalTo(bestSelection);
        times = 1;
      }
    };
  }
}
