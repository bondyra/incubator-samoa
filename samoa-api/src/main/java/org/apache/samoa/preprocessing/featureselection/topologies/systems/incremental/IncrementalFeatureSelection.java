package org.apache.samoa.preprocessing.featureselection.topologies.systems.incremental;

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

import com.github.javacliparser.ClassOption;
import com.github.javacliparser.Configurable;
import com.github.javacliparser.FlagOption;
import com.github.javacliparser.IntOption;
import org.apache.samoa.core.Processor;
import org.apache.samoa.instances.Instances;
import org.apache.samoa.learners.Learner;
import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.moa.classifiers.core.driftdetection.ChangeDetector;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRankerCreator;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.NominalIncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.NumericIncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.selectors.FeatureSelector;
import org.apache.samoa.preprocessing.featureselection.topologies.common.deciders.SelectionDecider;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.*;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.*;
import org.apache.samoa.topology.Stream;
import org.apache.samoa.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Incremental feature selection system that uses incrementally created rankings Optional adaptation by introducing
 * change detector node (flag -x)
 */
public class IncrementalFeatureSelection implements Learner, Configurable {
  private static final Logger logger = LoggerFactory.getLogger(IncrementalFeatureSelection.class);
  private static final long serialVersionUID = 0L;
  private IncrementalRankerCreator incrementalRankerCreator = new IncrementalRankerCreator();

  // adaptation specific options
  public FlagOption useChangeDetectorOption = new FlagOption("useChangeDetector",
      'x', "If not set, drift detection will be omitted and no adaptation is taken into account");
  public ClassOption changeDetectorOption = new ClassOption("changeDetector",
      'c', "Change drift detector algorithm.", ChangeDetector.class,
      "EDDM");
  public ClassOption ddModelOption = new ClassOption("ddModel",
      'm', "Base evaluating used by change detector.", LocalLearner.class,
      "NaiveBayes");
  public IntOption delayBetweenDriftsOption = new IntOption("delayBetweenDrifts",
      'z',
      "Delay between two drifts to be signalized by drift detector processor. Helpful if evaluations are ending prematurely",
      275, 0, Integer.MAX_VALUE);

  // aggregator node specific options
  public ClassOption splitterOption = new ClassOption("splitters",
      'l', "Instance splitter algorithm to use.", Splitter.class,
      "FixedInstanceSplitter");
  public ClassOption selectorOption = new ClassOption("selector",
      's', "Feature selector algorithm to use", FeatureSelector.class,
      "ThresholdFeatureSelector");
  public ClassOption deciderOption = new ClassOption("deciders",
      'd', "Decider algorithm to use", SelectionDecider.class,
      "BasicSelectionDecider");
  public IntOption eventsUntilSelectionOption = new IntOption("eventsUntilSelection",
      'n', "After how many Ranking content events selection will be performed?",
      1, 1, Integer.MAX_VALUE);

  // ranker node specific options
  public ClassOption numericRankingOption = new ClassOption("numericRanking",
      'u', "Numeric ranking algorithm to use.", NumericIncrementalRanker.class,
      "PearsonCorrelationIncrementalRanker");
  public ClassOption nominalRankingOption = new ClassOption("nominalRanking",
      'o', "Nominal ranking algorithm to use", NominalIncrementalRanker.class,
      "SymmetricalUncertaintyIncrementalRanker");
  public IntOption alternativeRankingPeriodOption = new IntOption(
      "alternativeRankingPeriod", 'a',
      "For how many instances after leaving the warning zone alternative rankingValues should last?",
      250, 1, Integer.MAX_VALUE);
  public IntOption rankingDelayOption = new IntOption(
      "rankingDelay", 'y', "How many messages should be processed before rankingValues event is computed?",
      300, 1, Integer.MAX_VALUE);

  // learner integration options
  public ClassOption featureAdaptiveLearnerOption = new ClassOption("FALearner",
      'f', "Type of learner to be integrated with feature selection system", Learner.class,
      "org.apache.samoa.learners.classifiers.ensemble.featureadaptation.DynamicFeatureLearner");

  private RankingAggregatorProcessor rankingAggregatorProcessor;
  private Learner learner;

  @Override
  public void init(TopologyBuilder topologyBuilder, Instances dataset, int parallelism) {
    Splitter splitter = splitterOption.getValue();
    splitter.setRankingIdentifiers(Collections.singletonList(1));
    rankingAggregatorProcessor = new RankingAggregatorProcessor.Builder()
        .splitter(splitter)
        .decider((SelectionDecider) deciderOption.getValue())
        .selector((FeatureSelector) selectorOption.getValue())
        .rankingRepository(new RankingRepository())
        .eventsUntilSelection(eventsUntilSelectionOption.getValue())
        .build();
    topologyBuilder.addProcessor(rankingAggregatorProcessor, parallelism);
    Stream rankerStream = topologyBuilder.createStream(rankingAggregatorProcessor);
    rankingAggregatorProcessor.setRankerStream(rankerStream);
    Stream outputStream = topologyBuilder.createStream(rankingAggregatorProcessor);
    rankingAggregatorProcessor.setOutputStream(outputStream);
    Stream instanceStream = topologyBuilder.createStream(rankingAggregatorProcessor);
    rankingAggregatorProcessor.setInstanceStream(instanceStream);

    Stream driftStream = null;
    if (useChangeDetectorOption.isSet()) {
      DriftDetectorProcessor ddProcessor = new DriftDetectorProcessor();
      topologyBuilder.addProcessor(ddProcessor, parallelism);
      ddProcessor.setLearner((LocalLearner) ddModelOption.getValue());
      ddProcessor.setChangeDetector((ChangeDetector) changeDetectorOption.getValue());
      ddProcessor.setDelayBetweenDrifts(delayBetweenDriftsOption.getValue());
      topologyBuilder.addProcessor(ddProcessor, parallelism);
      driftStream = topologyBuilder.createStream(ddProcessor);
      Stream unusedOutputStream = topologyBuilder.createStream(ddProcessor);
      ddProcessor.setDriftInfoStream(driftStream);
      ddProcessor.setOutputStream(unusedOutputStream);
      topologyBuilder.connectInputAllStream(instanceStream, ddProcessor);
    }

    RankerNodeProcessor featureRankerProcessor = new RankerNodeProcessor.Builder()
        .rankingWorkerRepository(getRankingWorkerRepository())
        .build();
    topologyBuilder.addProcessor(featureRankerProcessor, parallelism);
    Stream rankerOutputStream = topologyBuilder.createStream(featureRankerProcessor);
    featureRankerProcessor.setOutputStream(rankerOutputStream);
    // ranker processor inputs
    topologyBuilder.connectInputKeyStream(rankerStream, featureRankerProcessor);
    // append new input to master processor
    topologyBuilder.connectInputShuffleStream(rankerOutputStream, rankingAggregatorProcessor);

    learner = featureAdaptiveLearnerOption.getValue();
    learner.init(topologyBuilder, dataset, parallelism);
    //set inputs for learner - selections output stream and instance stream
    topologyBuilder.connectInputAllStream(rankingAggregatorProcessor.getOutputStream(), learner.getInputProcessor());
    topologyBuilder.connectInputAllStream(rankingAggregatorProcessor.getInstanceStream(), learner.getInputProcessor());

    // connect drift detection stream if it is used
    if (useChangeDetectorOption.isSet()) {
      topologyBuilder.connectInputAllStream(driftStream, rankingAggregatorProcessor);
      topologyBuilder.connectInputAllStream(driftStream, featureRankerProcessor);
    }

  }

  private RankingWorkerRepository getRankingWorkerRepository() {
    int alternativeRankingPeriod = alternativeRankingPeriodOption.getValue();
    Map<Integer, RankingWorker> workers = new HashMap<>();
    IncrementalRanker numeric = incrementalRankerCreator.getRanker(numericRankingOption.getValueAsCLIString());
    IncrementalRanker numericAlt = incrementalRankerCreator.getRanker(numericRankingOption.getValueAsCLIString());
    IncrementalRanker nominal = incrementalRankerCreator.getRanker(nominalRankingOption.getValueAsCLIString());
    IncrementalRanker nominalAlt = incrementalRankerCreator.getRanker(nominalRankingOption.getValueAsCLIString());
    workers.put(
        1,
        new IncrementalRankingWorker(
            1,
            numeric, nominal, numericAlt, nominalAlt,
            alternativeRankingPeriod, rankingDelayOption.getValue()));
    return new RankingWorkerRepository(workers);
  }

  @Override
  public Processor getInputProcessor() {
    return rankingAggregatorProcessor;
  }

  @Override
  public Set<Stream> getResultStreams() {
    return learner.getResultStreams();
  }
}