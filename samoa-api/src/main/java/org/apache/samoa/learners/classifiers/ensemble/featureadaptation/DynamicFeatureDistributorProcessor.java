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

import org.apache.samoa.core.ContentEvent;
import org.apache.samoa.core.Processor;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.InstanceContentEvent;
import org.apache.samoa.learners.ResetContentEvent;
import org.apache.samoa.learners.classifiers.ensemble.ShardingDistributorProcessor;
import org.apache.samoa.moa.core.MiscUtils;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilterCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Dynamic Feature Learner distributor processor. It distributes instances using on-line bagging. It also filters
 * instances basing on feature selection information.
 */
public class DynamicFeatureDistributorProcessor extends ShardingDistributorProcessor {
  /**
   * Stream used to pass selection events to combiner
   */
  private Stream featureSelectionStream;

  /**
   * Used for dynamic filter creation
   */
  private FeatureFilterCreator featureFilterCreator;
  private Map<Integer, FeatureFilter> filters = new HashMap<>();

  private Logger logger = LoggerFactory.getLogger(DynamicFeatureDistributorProcessor.class);

  @Override
  public boolean process(ContentEvent event) {
    try {
      if (event instanceof SelectionContentEvent) {
        // pass selection to combiner
        featureSelectionStream.put(event);
        return false;
      } else if (event instanceof SelectFeaturesContentEvent) {
        SelectFeaturesContentEvent selectFeaturesEvent = (SelectFeaturesContentEvent) event;
        // iterate through ensemble members identifiers to update their instance filters
        for (int ensembleId : selectFeaturesEvent.getEnsembleStreamIds()) {
          FeatureFilter filter = filters.containsKey(ensembleId) ? filters.get(ensembleId)
              : featureFilterCreator.getFilter();
          // update instance filter
          filter.setSelection(selectFeaturesEvent.getSelection());
          filters.put(ensembleId, filter);
          // request reset of ensemble member
          ensembleStreams[ensembleId].put(new ResetContentEvent());
        }
      } else if (event instanceof InstanceContentEvent) {
        return processInstance((InstanceContentEvent) event);
      }
    } catch (IllegalStateException e) {
      logger.error("Exception during event processing " + e.getMessage());
    }
    return false;
  }

  protected boolean processInstance(InstanceContentEvent event) throws IllegalStateException {
    if (event.isLastEvent()) {
      // end learning
      for (Stream stream : ensembleStreams)
        stream.put(event);
      return false;
    }

    if (event.isTesting()) {
      Instance testInstance = event.getInstance();
      for (int i = 0; i < ensembleSize; i++) {
        // filter the instance if member has assigned selection
        Instance instanceCopy = filters.containsKey(i) ? filters.get(i).processInstance(testInstance)
            : testInstance.copy();

        InstanceContentEvent instanceContentEvent = new InstanceContentEvent(event.getInstanceIndex(), instanceCopy,
            false, true);
        instanceContentEvent.setClassifierIndex(i);
        instanceContentEvent.setEvaluationIndex(event.getEvaluationIndex());
        ensembleStreams[i].put(instanceContentEvent);
      }
    }

    // estimate evaluating parameters using the training data
    if (event.isTraining()) {
      train(event);
    }
    return false;
  }

  @Override
  protected void train(InstanceContentEvent inEvent) {
    Instance trainInstance = inEvent.getInstance();
    for (int i = 0; i < ensembleSize; i++) {
      // if there is one member, don't use bagging at all
      int k = ensembleSize == 1 ? 1 : MiscUtils.poisson(1.0, this.random);
      if (k > 0) {
        // filter the instance if member has assigned selection
        Instance weightedInstance = filters.containsKey(i) ? filters.get(i).processInstance(trainInstance)
            : trainInstance.copy();

        weightedInstance.setWeight(trainInstance.weight() * k);
        InstanceContentEvent instanceContentEvent = new InstanceContentEvent(inEvent.getInstanceIndex(),
            weightedInstance, true, false);
        instanceContentEvent.setClassifierIndex(i);
        instanceContentEvent.setEvaluationIndex(inEvent.getEvaluationIndex());
        ensembleStreams[i].put(instanceContentEvent);
      }
    }
  }

  @Override
  public Processor newProcessor(Processor sourceProcessor) {
    DynamicFeatureDistributorProcessor newProcessor = new DynamicFeatureDistributorProcessor();
    DynamicFeatureDistributorProcessor originProcessor = (DynamicFeatureDistributorProcessor) sourceProcessor;
    if (originProcessor.getOutputStreams() != null) {
      newProcessor.setOutputStreams(Arrays.copyOf(originProcessor.getOutputStreams(),
          originProcessor.getOutputStreams().length));
    }
    newProcessor.featureFilterCreator = originProcessor.featureFilterCreator;
    newProcessor.setEnsembleSize(originProcessor.getEnsembleSize());
    newProcessor.setFeatureSelectionStream(originProcessor.featureSelectionStream);
    return newProcessor;
  }

  public void setFeatureSelectionStream(Stream featureSelectionStream) {
    this.featureSelectionStream = featureSelectionStream;
  }

  public void setFeatureFilterCreator(FeatureFilterCreator featureFilterCreator) {
    this.featureFilterCreator = featureFilterCreator;
  }
}
