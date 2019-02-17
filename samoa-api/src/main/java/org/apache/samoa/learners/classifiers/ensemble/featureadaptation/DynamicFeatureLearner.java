package org.apache.samoa.learners.classifiers.ensemble.featureadaptation;

/*
 * #%L
 * SAMOA
 * %%
 * Copyright (C) 2014 - 2015 Apache Software Foundation
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

import com.github.javacliparser.IntOption;
import org.apache.samoa.learners.Learner;
import org.apache.samoa.learners.classifiers.ensemble.Sharding;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilterCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethodCreator;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javacliparser.ClassOption;

/**
 * Dynamic Feature Learner algorithm. A variation of standard Bagging algorithm that integrates with feature selection
 * systems. When new information about relevant features arrives, it resets some of the ensemble members.
 */
public class DynamicFeatureLearner extends Sharding {
  private static final Logger logger = LoggerFactory.getLogger(DynamicFeatureLearner.class);

  public IntOption instanceRandomSeedOption = new IntOption("instanceRandomSeed", 'i',
      "Seed for random reset process of ensemble members", 1);

  public IntOption learnerGracePeriodOption = new IntOption("learnerGracePeriod", 'g',
      "For how many instances a learner after reset won't be reset again?", 2000);

  @Override
  protected void setLayout() {
    int ensembleSize = this.ensembleSizeOption.getValue();

    distributor = new DynamicFeatureDistributorProcessor();
    distributor.setEnsembleSize(ensembleSize);
    ((DynamicFeatureDistributorProcessor) distributor).setFeatureFilterCreator(new FeatureFilterCreator());
    this.builder.addProcessor(distributor, 1);

    // instantiate ensemble members
    ensemble = new Learner[ensembleSize];
    for (int i = 0; i < ensembleSize; i++) {
      try {
        ensemble[i] = (Learner) ClassOption.createObject(baseLearnerOption.getValueAsCLIString(),
            baseLearnerOption.getRequiredType());
      } catch (Exception e) {
        logger.error("Unable to create members of the ensemble. Please check your CLI parameters");
        e.printStackTrace();
        throw new IllegalArgumentException(e);
      }
      ensemble[i].init(builder, this.dataset, 1); // sequential
    }

    DynamicFeaturePredictionCombinerProcessor predictionCombiner = new DynamicFeaturePredictionCombinerProcessor();
    predictionCombiner.setEnsembleSize(ensembleSize);
    EnsembleResetDecider ensembleResetDecider = new EnsembleResetDecider(instanceRandomSeedOption.getValue(),
        learnerGracePeriodOption.getValue());
    ensembleResetDecider.setMethodCreator(new EvaluationMethodCreator());
    predictionCombiner.setEnsembleResetDecider(ensembleResetDecider);
    this.builder.addProcessor(predictionCombiner, 1);

    // Streams
    resultStream = this.builder.createStream(predictionCombiner);
    predictionCombiner.setOutputStream(resultStream);
    // feedback from combiner to distributor
    Stream distributorStream = this.builder.createStream(predictionCombiner);
    predictionCombiner.setDistributorStream(distributorStream);
    this.builder.connectInputAllStream(distributorStream, distributor);
    // stream to pass selection content events from distributor to combiner
    Stream selectionStream = this.builder.createStream(distributor);
    ((DynamicFeatureDistributorProcessor) distributor).setFeatureSelectionStream(selectionStream);
    this.builder.connectInputAllStream(selectionStream, predictionCombiner);

    for (Learner member : ensemble) {
      for (Stream subResultStream : member.getResultStreams()) { // a learner can have multiple output streams
        this.builder.connectInputKeyStream(subResultStream, predictionCombiner); // the key is the instance id to combine predictions
      }
    }

    ensembleStreams = new Stream[ensembleSize];
    for (int i = 0; i < ensembleSize; i++) {
      ensembleStreams[i] = builder.createStream(distributor);
      builder.connectInputShuffleStream(ensembleStreams[i], ensemble[i].getInputProcessor()); // connect streams one-to-one with ensemble members (the type of connection does not matter)
    }

    distributor.setOutputStreams(ensembleStreams);
  }
}
