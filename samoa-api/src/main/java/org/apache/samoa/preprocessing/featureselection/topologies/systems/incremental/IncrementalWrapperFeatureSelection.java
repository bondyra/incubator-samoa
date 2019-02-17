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
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilterCreator;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRankerCreator;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.NominalIncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.NumericIncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.selectors.FeatureSelector;
import org.apache.samoa.preprocessing.featureselection.topologies.common.deciders.EvaluationDecider;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorkerCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.LocalLearnerCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethodCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.IncrementalRankingWorker;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingWorker;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.DriftDetectorProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.WrapperAggregatorProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.EvaluatorNodeProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.RankerNodeProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.topology.Stream;
import org.apache.samoa.topology.TopologyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Incremental wrapper feature selection system Extension of IncrementalFeatureSelection - includes wrapper feature
 * selection
 */
public class IncrementalWrapperFeatureSelection implements Learner, Configurable {
  private static final Logger logger = LoggerFactory.getLogger(IncrementalWrapperFeatureSelection.class);
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
      'b',
      "Delay between two drifts to be signalized by drift detector processor. Helpful if evaluations are ending prematurely",
      275, 0, Integer.MAX_VALUE);

  // aggregator node specific options
  public ClassOption splitterOption = new ClassOption("splitter",
      'l', "Instance splitters to use", Splitter.class,
      "FixedInstanceSplitter");
  public ClassOption selectorOption = new ClassOption("selector",
      's', "Feature selector algorithm to use", FeatureSelector.class,
      "ThresholdFeatureSelector");
  public ClassOption deciderOption = new ClassOption("deciders",
      'd', "Selection sharing decision algorithm to use", EvaluationDecider.class,
      "BasicEvaluationDecider");
  public IntOption eventsUntilSelectionOption = new IntOption("eventsUntilSelection",
      'n', "After how many ranking content events selection will be performed?",
      1, 1, Integer.MAX_VALUE);

  // ranker node specific options
  public ClassOption numericRankingOption = new ClassOption("numericRanking",
      'u', "Numeric ranking algorithm to use.", NumericIncrementalRanker.class,
      "PearsonCorrelationIncrementalRanker");
  public ClassOption nominalRankingOption = new ClassOption("nominalRanking",
      'o', "Nominal ranking algorithm to use.", NominalIncrementalRanker.class,
      "SymmetricalUncertaintyIncrementalRanker");
  public IntOption alternativeRankingPeriodOption = new IntOption(
      "alternativeRankingPeriod", 'a',
      "For how many instances after leaving the warning zone alternative rankingValues should last?",
      250, 1, Integer.MAX_VALUE);
  public IntOption rankingDelayOption = new IntOption(
      "rankingDelay", 'y', "How many messages should be processed before ranking event is computed?",
      300, 1, Integer.MAX_VALUE);

  // wrapper feature selection specific options
  public ClassOption evaluationLearnerOption = new ClassOption("evaluationLearner",
      'e', "Which learner wil be used in wrapper method?", LocalLearner.class,
      "NaiveBayes");

  public ClassOption evaluationMethodOption = new ClassOption("evaluationMetric",
      'v', "Which evaluation method wil be used in wrapper method?", EvaluationMethod.class,
      "AccuracyEvaluationMethod");

  public IntOption evaluationPeriodOption = new IntOption("evaluationPeriod",
      'p', "For how many instances evaluation of features should be performed?",
      250, 1, Integer.MAX_VALUE);

  // learner integration options
  public ClassOption featureAdaptiveLearnerOption = new ClassOption("FALearner",
      'f', "Type of learner to be integrated with feature selection system", Learner.class,
      "org.apache.samoa.learners.classifiers.SingleClassifier");

  private WrapperAggregatorProcessor wrapperAggregatorProcessor;
  private Learner learner;

  @Override
  public void init(TopologyBuilder topologyBuilder, Instances dataset, int parallelism) {
    Splitter splitter = splitterOption.getValue();
    splitter.setRankingIdentifiers(Collections.singletonList(1));
    wrapperAggregatorProcessor = new WrapperAggregatorProcessor.Builder()
        .splitter(splitter)
        .decider((EvaluationDecider) deciderOption.getValue())
        .selectors(Collections.singletonList((FeatureSelector) selectorOption.getValue()))
        .evaluationRepository(new EvaluationRepository())
        .rankingRepository(new RankingRepository())
        .eventsUntilSelection(eventsUntilSelectionOption.getValue())
        .build();
    topologyBuilder.addProcessor(wrapperAggregatorProcessor, parallelism);
    Stream rankerStream = topologyBuilder.createStream(wrapperAggregatorProcessor);
    wrapperAggregatorProcessor.setRankerStream(rankerStream);
    Stream evaluatorStream = topologyBuilder.createStream(wrapperAggregatorProcessor);
    wrapperAggregatorProcessor.setEvaluatorStream(evaluatorStream);
    Stream outputStream = topologyBuilder.createStream(wrapperAggregatorProcessor);
    wrapperAggregatorProcessor.setOutputStream(outputStream);
    Stream instanceStream = topologyBuilder.createStream(wrapperAggregatorProcessor);
    wrapperAggregatorProcessor.setInstanceStream(instanceStream);

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
    topologyBuilder.connectInputShuffleStream(rankerOutputStream, wrapperAggregatorProcessor);

    EvaluatorNodeProcessor evaluatorNodeProcessor = new EvaluatorNodeProcessor.Builder()
        .evaluationPeriod(evaluationPeriodOption.getValue())
        .evaluationWorkerRepository(new EvaluationWorkerRepository())
        .localLearnerCreator(new LocalLearnerCreator(evaluationLearnerOption.getValueAsCLIString()))
        .featureFilterCreator(new FeatureFilterCreator())
        .evaluationMethodCreator(new EvaluationMethodCreator(evaluationMethodOption.getValueAsCLIString()))
        .evaluationWorkerCreator(new EvaluationWorkerCreator())
        .build();
    topologyBuilder.addProcessor(evaluatorNodeProcessor, parallelism);
    Stream evaluatorOutputStream = topologyBuilder.createStream(evaluatorNodeProcessor);
    evaluatorNodeProcessor.setOutputStream(evaluatorOutputStream);
    // evaluator processor inputs
    topologyBuilder.connectInputShuffleStream(evaluatorStream, evaluatorNodeProcessor);
    topologyBuilder.connectInputAllStream(instanceStream, evaluatorNodeProcessor);
    // append new input to master processor
    topologyBuilder.connectInputShuffleStream(evaluatorOutputStream, wrapperAggregatorProcessor);

    learner = featureAdaptiveLearnerOption.getValue();
    learner.init(topologyBuilder, dataset, parallelism);
    //set inputs for learner - selections output stream and instance stream
    topologyBuilder
        .connectInputAllStream(wrapperAggregatorProcessor.getOutputStream(), learner.getInputProcessor());
    topologyBuilder
        .connectInputAllStream(wrapperAggregatorProcessor.getInstanceStream(), learner.getInputProcessor());

    // connect drift detection stream if it is used
    if (useChangeDetectorOption.isSet()) {
      topologyBuilder.connectInputAllStream(driftStream, wrapperAggregatorProcessor);
      topologyBuilder.connectInputAllStream(driftStream, featureRankerProcessor);
      topologyBuilder.connectInputAllStream(driftStream, evaluatorNodeProcessor);
    }
  }

  private RankingWorkerRepository getRankingWorkerRepository() {
    int alternativeRankingPeriod = alternativeRankingPeriodOption.getValue();
    Map<Integer, RankingWorker> workers = new HashMap<>();
    IncrementalRanker numeric = incrementalRankerCreator
        .getRanker(numericRankingOption.getValueAsCLIString());
    IncrementalRanker numericAlt = incrementalRankerCreator
        .getRanker(numericRankingOption.getValueAsCLIString());
    IncrementalRanker nominal = incrementalRankerCreator
        .getRanker(nominalRankingOption.getValueAsCLIString());
    IncrementalRanker nominalAlt = incrementalRankerCreator
        .getRanker(nominalRankingOption.getValueAsCLIString());
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
    return wrapperAggregatorProcessor;
  }

  @Override
  public Set<Stream> getResultStreams() {
    return learner.getResultStreams();
  }
}