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

import com.github.javacliparser.*;
import org.apache.samoa.core.Processor;
import org.apache.samoa.instances.Instances;
import org.apache.samoa.learners.Learner;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRankerCreator;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.NominalIncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.NumericIncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.selectors.FeatureSelector;
import org.apache.samoa.preprocessing.featureselection.topologies.common.deciders.SelectionDecider;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.BatchRankingWorker;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingWorker;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.RankingAggregatorProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.common.splitters.Splitter;
import org.apache.samoa.preprocessing.featureselection.topologies.processors.RankerNodeProcessor;
import org.apache.samoa.topology.Stream;
import org.apache.samoa.topology.TopologyBuilder;

import java.util.*;

/**
 * Batch feature selection system Uses ranking selection method computed on batches (size set with -b option)
 */
public class BatchFeatureSelection implements Learner, Configurable {
  private static final long serialVersionUID = 0L;
  private IncrementalRankerCreator incrementalRankerCreator = new IncrementalRankerCreator();

  // aggregator node specific options
  public ClassOption splitterOption = new ClassOption("splitters",
      'l', "Instance splitter algorithm to use", Splitter.class,
      "FixedInstanceSplitter");
  public ClassOption selectorOption = new ClassOption("selector",
      's', "Feature selector algorithm to use", FeatureSelector.class,
      "ThresholdFeatureSelector");
  public ClassOption deciderOption = new ClassOption("deciders",
      'd', "Selection sharing decision algorithm to use", SelectionDecider.class,
      "BasicSelectionDecider");
  public IntOption eventsUntilSelectionOption = new IntOption("eventsUntilSelection",
      'n', "After how many Ranking content events selection will be performed?",
      1, 1, Integer.MAX_VALUE);

  // ranker node specific options
  public ClassOption numericRankingOption = new ClassOption("numericRanking",
      'u', "Numeric ranking algorithm to use", NumericIncrementalRanker.class,
      "PearsonCorrelationIncrementalRanker");
  public ClassOption nominalRankingOption = new ClassOption("nominalRanking",
      'o', "Nominal ranking algorithm to use", NominalIncrementalRanker.class,
      "SymmetricalUncertaintyIncrementalRanker");
  public IntOption batchSizeOption = new IntOption(
      "batchSize", 'b', "Ranking batch size",
      1000, 1, Integer.MAX_VALUE);

  // learner integration options
  public ClassOption featureAdaptiveLearnerOption = new ClassOption("FALearner",
      'f', "Type of learner to be integrated with feature selection system", Learner.class,
      "org.apache.samoa.learners.classifiers.SingleClassifier");

  private RankingAggregatorProcessor rankingAggregatorProcessor;
  private Learner learner;

  @Override
  public void init(TopologyBuilder topologyBuilder, Instances dataset, int parallelism) {
    Splitter splitter = splitterOption.getValue();
    splitter.setRankingIdentifiers(this.getRankingIdentifiers());
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

    RankerNodeProcessor featureRankerProcessor = new RankerNodeProcessor.Builder()
        .rankingWorkerRepository(getRankingWorkerRepository())
        .build();
    topologyBuilder.addProcessor(featureRankerProcessor, parallelism);
    Stream featureRankerStream = topologyBuilder.createStream(featureRankerProcessor);
    featureRankerProcessor.setOutputStream(featureRankerStream);
    // add input to ranker from master processor
    topologyBuilder.connectInputKeyStream(rankerStream, featureRankerProcessor);
    // add input to master processor from ranker
    topologyBuilder.connectInputShuffleStream(featureRankerStream, rankingAggregatorProcessor);
    // learner specific builder procedure:
    learner = featureAdaptiveLearnerOption.getValue();
    learner.init(topologyBuilder, dataset, parallelism);
    //set inputs for learner - selections output stream and instance stream
    topologyBuilder.connectInputAllStream(rankingAggregatorProcessor.getOutputStream(), learner.getInputProcessor());
    topologyBuilder.connectInputAllStream(rankingAggregatorProcessor.getInstanceStream(), learner.getInputProcessor());
  }

  private List<Integer> getRankingIdentifiers() {
    return Collections.singletonList(1);
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
    return rankingAggregatorProcessor;
  }

  @Override
  public Set<Stream> getResultStreams() {
    return learner.getResultStreams();
  }
}