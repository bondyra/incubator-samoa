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

import org.apache.samoa.core.ContentEvent;
import org.apache.samoa.core.Processor;
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
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationContentEvent;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Central processor for wrapper-oriented feature selection systems. Orchestrates wrapper feature selection processes.
 */
public class WrapperAggregatorProcessor implements Processor {
  private EvaluationDecider decider;
  private Collection<FeatureSelector> selectors;
  private Splitter splitter;

  private RankingRepository rankingRepository;
  private EvaluationRepository evaluationRepository;

  /**
   * Keyed stream, put partial instance information for ranking computation
   */
  private Stream rankerStream;
  /**
   * Broadcast stream, put selections for external algorithms
   */
  private Stream outputStream;
  /**
   * Broadcast stream, pass instances to components that require them
   */
  private Stream instanceStream;
  /**
   * Shuffle stream, put evaluation requests for evaluators
   */
  private Stream evaluatorStream;

  /**
   * Indicates how many ranking result messages must be acquired in order to perform selection (and start evaluation)
   */
  private int eventsUntilSelection;

  private Logger logger = LoggerFactory.getLogger(WrapperAggregatorProcessor.class);

  public WrapperAggregatorProcessor(Builder builder) {
    this.decider = builder.decider;
    this.selectors = builder.selectors;
    this.rankingRepository = builder.rankingRepository;
    this.evaluationRepository = builder.evaluationRepository;
    this.splitter = builder.splitter;
    this.eventsUntilSelection = builder.eventsUntilSelection;
  }

  @Override
  public boolean process(ContentEvent event) {
    try {
      if (event instanceof InstanceContentEvent) {
        instanceStream.put(event); // pass the information to components that require it
        handleInstanceContentEvent((InstanceContentEvent) event);
      } else if (event instanceof RankingContentEvent) {
        handleRankingContentEvent((RankingContentEvent) event);
      } else if (event instanceof EvaluationResultContentEvent) {
        handleEvaluationContentEvent((EvaluationResultContentEvent) event);
      } else if (event instanceof WarningContentEvent) {
        handleWarningContentEvent((WarningContentEvent) event);
      } else if (event instanceof DriftContentEvent) {
        handleDriftContentEvent((DriftContentEvent) event);
      }
    } catch (IllegalAccessException e) {
      logger.error("Exception during event processing: " + e.getMessage());
    }
    return false;
  }

  @Override
  public void onCreate(int id) {
    this.decider.setDecisionStream(outputStream);
  }

  @Override
  public Processor newProcessor(Processor processor) {
    WrapperAggregatorProcessor oldProcessor = (WrapperAggregatorProcessor) processor;
    WrapperAggregatorProcessor newProcessor = new WrapperAggregatorProcessor.Builder(oldProcessor).build();

    newProcessor.setEvaluatorStream(oldProcessor.evaluatorStream);
    newProcessor.setRankerStream(oldProcessor.rankerStream);
    newProcessor.setOutputStream(oldProcessor.outputStream);
    newProcessor.setInstanceStream(oldProcessor.instanceStream);
    return newProcessor;
  }

  /**
   * Naive drift handling method - resign from all evaluations
   * 
   * @param event
   *          drift signal
   */
  private void handleDriftContentEvent(DriftContentEvent event) {
    evaluationRepository.purge();
  }

  /**
   * Naive drift handling method - ignore it
   * 
   * @param event
   *          warning signal
   */
  private void handleWarningContentEvent(WarningContentEvent event) {
  }

  /**
   * Process ranking results and periodically perform selection, start evaluations if necessary
   * 
   * @param event
   *          ranking result information
   * @throws IllegalAccessException
   */
  private void handleRankingContentEvent(RankingContentEvent event) throws IllegalAccessException {
    rankingRepository.aggregateEvent(event);
    int rankingId = event.getRankingIdentifier();
    int eventsProcessed = rankingRepository.getEventsProcessed(rankingId);
    if (eventsProcessed % eventsUntilSelection == 0) {
      InstanceRanking instanceRanking = rankingRepository.getInstanceRanking(rankingId);

      // gather selections that should be evaluated
      List<Selection> newSelections = new LinkedList<>();
      Selection bestSelection = decider.getBestSelection();
      for (FeatureSelector selector : selectors) {
        for (Selection selection : selector.selectFeatures(instanceRanking)) {
          if (!selection.equalTo(bestSelection)) // no point in adding otherwise
            newSelections.add(selection);
        }
      }

      // put evaluation requests
      if (newSelections.size() > 0) { // no point in evaluation if no new selection is different from current best
        if (bestSelection != null) // add current best to refresh its grade
          newSelections.add(bestSelection);
        // evaluations from one occurrence of selection are evaluated in group
        int groupId = evaluationRepository.createEvaluationGroup(newSelections);
        List<Evaluation> evaluationGroup = evaluationRepository.getEvaluationGroup(groupId);
        for (Evaluation evaluation : evaluationGroup) {
          evaluatorStream.put(new EvaluationContentEvent(evaluation));
        }
      }
    }
  }

  /**
   * Process evaluation result, if whole group evaluation has ended - decide if new selection should be shared
   * 
   * @param event
   *          evaluation result information
   * @throws IllegalAccessException
   */
  private void handleEvaluationContentEvent(EvaluationResultContentEvent event) throws IllegalAccessException {
    int evaluationId = event.getEvaluationId();
    boolean groupPrepared = evaluationRepository.updateEvaluationResult(evaluationId, event.getEvaluationResult(),
        event.getEvaluationUpperBound());
    if (groupPrepared) {
      int evaluationGroupId = evaluationRepository.getGroupByEvaluationId(evaluationId);
      List<Evaluation> entries = evaluationRepository.getEvaluationGroup(evaluationGroupId);
      decider.processEvaluationResults(entries);
      evaluationRepository.removeEvaluationGroup(evaluationGroupId);
    }
  }

  /**
   * Split instance into chunks according to algorithm and put them to be processed by rankers
   * 
   * @param event
   */
  private void handleInstanceContentEvent(InstanceContentEvent event) {
    for (FeatureSetContentEvent featureEvent : splitter.split(event.getInstance())) {
      rankerStream.put(featureEvent);
    }
  }

  public void setRankerStream(Stream stream) {
    this.rankerStream = stream;
  }

  public void setEvaluatorStream(Stream stream) {
    this.evaluatorStream = stream;
  }

  public Stream getOutputStream() {
    return this.outputStream;
  }

  public void setOutputStream(Stream outputStream) {
    this.outputStream = outputStream;
  }

  public Stream getInstanceStream() {
    return instanceStream;
  }

  public void setInstanceStream(Stream instanceStream) {
    this.instanceStream = instanceStream;
  }

  /**
   * Builder of WrapperAggregatorProcessor that handles all configurable parameters
   */
  public static class Builder {
    private EvaluationDecider decider;
    private Collection<FeatureSelector> selectors;
    private Splitter splitter;
    private RankingRepository rankingRepository;
    private EvaluationRepository evaluationRepository;

    private int eventsUntilSelection;

    public Builder() {
    }

    public Builder(WrapperAggregatorProcessor oldProcessor) {
      this.decider = oldProcessor.decider;
      this.selectors = oldProcessor.selectors;
      this.splitter = oldProcessor.splitter;
      this.rankingRepository = oldProcessor.rankingRepository;
      this.evaluationRepository = oldProcessor.evaluationRepository;
      this.eventsUntilSelection = oldProcessor.eventsUntilSelection;
    }

    public Builder splitter(Splitter splitter) {
      this.splitter = splitter;
      return this;
    }

    public Builder selectors(Collection<FeatureSelector> selectors) {
      this.selectors = selectors;
      return this;
    }

    public Builder decider(EvaluationDecider decider) {
      this.decider = decider;
      return this;
    }

    public Builder evaluationRepository(EvaluationRepository repository) {
      this.evaluationRepository = repository;
      return this;
    }

    public Builder rankingRepository(RankingRepository repository) {
      this.rankingRepository = repository;
      return this;
    }

    public Builder eventsUntilSelection(int eventsUntilSelection) {
      this.eventsUntilSelection = eventsUntilSelection;
      return this;
    }

    public WrapperAggregatorProcessor build() {
      return new WrapperAggregatorProcessor(this);
    }
  }
}
