package org.apache.samoa.preprocessing.featureselection.topologies.systems.batch;

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
import com.github.javacliparser.IntOption;
import org.apache.samoa.core.Processor;
import org.apache.samoa.instances.Instances;
import org.apache.samoa.learners.Learner;
import org.apache.samoa.learners.classifiers.LocalLearner;
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
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.*;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.EvaluatorNodeProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.RankerNodeProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.WrapperAggregatorProcessor;
import org.apache.samoa.topology.Stream;
import org.apache.samoa.topology.TopologyBuilder;

import java.util.*;

/**
 * Batch wrapper feature selection system Extension of BatchFeatureSelection - includes wrapper feature selection
 */
public class BatchWrapperFeatureSelection implements Learner, Configurable {
  private static final long serialVersionUID = 0L;
  private IncrementalRankerCreator incrementalRankerCreator = new IncrementalRankerCreator();

  // aggregator node specific options
  public ClassOption splitterOption = new ClassOption("splitter",
      'l', "Instance splitter algorithm to use.", Splitter.class,
      "FixedInstanceSplitter");
  public ClassOption selectionOption = new ClassOption("selector",
      's', "Feature selector algorithm to use.", FeatureSelector.class,
      "ThresholdFeatureSelector");
  public ClassOption deciderOption = new ClassOption("deciders",
      'd', "Decider algorithm to use.", EvaluationDecider.class,
      "BasicEvaluationDecider");
  public IntOption eventsUntilSelectionOption = new IntOption("eventsUntilSelection",
      'n', "After how many Ranking content events selection will be performed?",
      1, 1, Integer.MAX_VALUE);

  // ranker node specific options
  public ClassOption numericRankingOption = new ClassOption("numericRanking",
      'u', "Numeric ranking algorithm to use.", NumericIncrementalRanker.class,
      "PearsonCorrelationIncrementalRanker");
  public ClassOption nominalRankingOption = new ClassOption("nominalRanking",
      'o', "Nominal ranking algorithm to use.", NominalIncrementalRanker.class,
      "SymmetricalUncertaintyIncrementalRanker");
  public IntOption batchSizeOption = new IntOption(
      "batchSize", 'b', "Ranking batch size",
      1000, 1, Integer.MAX_VALUE);

  // wrapper feature selection specific options
  public ClassOption evaluationLearnerOption = new ClassOption("evaluationLearner",
      'e', "Which learner wil be used in wrapper method?", LocalLearner.class,
      "NaiveBayes");

  public ClassOption evaluationMethodOption = new ClassOption("evaluationMetric",
      'v', "Which evaluation method wil be used in wrapper method?", EvaluationMethod.class,
      "AccuracyEvaluationMethod");

  public IntOption evaluationPeriodOption = new IntOption("evaluationPeriod",
      'p', "For how many instances evaluation of features should be performed?",
      900, 1, Integer.MAX_VALUE);

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
        .selectors(Collections.singletonList((FeatureSelector) selectionOption.getValue()))
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
    topologyBuilder.connectInputAllStream(wrapperAggregatorProcessor.getOutputStream(), learner.getInputProcessor());
    topologyBuilder.connectInputAllStream(wrapperAggregatorProcessor.getInstanceStream(), learner.getInputProcessor());
  }

  private RankingWorkerRepository getRankingWorkerRepository() {
    int rankingId = 1;
    Map<Integer, RankingWorker> workers = new HashMap<>();
    IncrementalRanker numeric = incrementalRankerCreator.getRanker(numericRankingOption.getValueAsCLIString());
    IncrementalRanker nominal = incrementalRankerCreator.getRanker(nominalRankingOption.getValueAsCLIString());
    workers.put(
        rankingId,
        new BatchRankingWorker(rankingId, batchSizeOption.getValue(), numeric, nominal));
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