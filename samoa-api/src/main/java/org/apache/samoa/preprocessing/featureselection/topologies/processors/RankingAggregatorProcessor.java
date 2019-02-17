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
import org.apache.samoa.preprocessing.featureselection.ranking.selectors.FeatureSelector;
import org.apache.samoa.preprocessing.featureselection.topologies.common.deciders.SelectionDecider;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.preprocessing.featureselection.topologies.events.*;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central processor for feature selection systems. Synchronizes ranking computation, coordinates drift signalization
 * process. Selects features from ranking and passes selection information for external algorithms
 */
public class RankingAggregatorProcessor implements Processor {
  private SelectionDecider decider;
  private FeatureSelector selector;
  private Splitter splitter;

  private RankingRepository rankingRepository;

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
   * Indicates how many ranking result messages must be acquired in order to perform selection (and start evaluation)
   */
  private int eventsUntilSelection;

  private Logger logger = LoggerFactory.getLogger(RankingAggregatorProcessor.class);

  public RankingAggregatorProcessor(Builder builder) {
    this.decider = builder.decider;
    this.selector = builder.selector;
    this.splitter = builder.splitter;
    this.rankingRepository = builder.rankingRepository;
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
      }
    } catch (IllegalAccessException e) {
      logger.error("Exception during event processing. " + e.getMessage());
    }
    return false;
  }

  @Override
  public void onCreate(int id) {
    this.decider.setDecisionStream(outputStream);
  }

  @Override
  public Processor newProcessor(Processor processor) {
    RankingAggregatorProcessor oldProcessor = (RankingAggregatorProcessor) processor;
    RankingAggregatorProcessor newProcessor = new RankingAggregatorProcessor.Builder(oldProcessor).build();

    newProcessor.setRankerStream(oldProcessor.rankerStream);
    newProcessor.setOutputStream(oldProcessor.outputStream);
    newProcessor.setInstanceStream(oldProcessor.instanceStream);
    return newProcessor;
  }

  /**
   * Process ranking results and periodically perform selection
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
      decider.processSelections(selector.selectFeatures(instanceRanking), instanceRanking);
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
   * Builder of RankingAggregatorProcessor that handles all configurable parameters
   */
  public static class Builder {
    private SelectionDecider decider;
    private FeatureSelector selector;
    private Splitter splitter;
    private RankingRepository rankingRepository;
    private int eventsUntilSelection;

    public Builder() {
    }

    public Builder(RankingAggregatorProcessor oldProcessor) {
      this.decider = oldProcessor.decider;
      this.selector = oldProcessor.selector;
      this.splitter = oldProcessor.splitter;
      this.rankingRepository = oldProcessor.rankingRepository;
      this.eventsUntilSelection = oldProcessor.eventsUntilSelection;
    }

    public Builder splitter(Splitter splitter) {
      this.splitter = splitter;
      return this;
    }

    public Builder selector(FeatureSelector selector) {
      this.selector = selector;
      return this;
    }

    public Builder decider(SelectionDecider decider) {
      this.decider = decider;
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

    public RankingAggregatorProcessor build() {
      return new RankingAggregatorProcessor(this);
    }
  }
}
