package org.apache.samoa.learners.classifiers.ensemble.featureadaptation;

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
import org.apache.samoa.learners.ResultContentEvent;
import org.apache.samoa.learners.classifiers.ensemble.PredictionCombinerProcessor;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Dynamic Feature Learner prediction combiner processor. It acts as standard PredictionCombinerProcessor, but it also
 * considers selections acquired from feature selection system, that are used for resetting some of the members. Reset
 * procedure is contained in this processor.
 */
public class DynamicFeaturePredictionCombinerProcessor extends PredictionCombinerProcessor {
  private Logger logger = LoggerFactory.getLogger(DynamicFeaturePredictionCombinerProcessor.class);

  /**
   * Used to pass information how to filter instances during distribution
   */
  private Stream distributorStream;

  /**
   * Decides which ensemble members should be reset after arrival of new feature selection event
   */
  private EnsembleResetDecider ensembleResetDecider;

  public boolean process(ContentEvent event) {
    try {
      if (event instanceof SelectionContentEvent) {
        processSelection((SelectionContentEvent) event);
      } else if (event instanceof ResultContentEvent) {
        ensembleResetDecider.processResultContentEvent((ResultContentEvent) event);
        return super.process(event);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Exception during event processing. " + e.getMessage());
    }
    return false;
  }

  @Override
  public Processor newProcessor(Processor sourceProcessor) {
    DynamicFeaturePredictionCombinerProcessor newProcessor = new DynamicFeaturePredictionCombinerProcessor();
    DynamicFeaturePredictionCombinerProcessor originProcessor = (DynamicFeaturePredictionCombinerProcessor) sourceProcessor;
    if (originProcessor.getOutputStream() != null) {
      newProcessor.setOutputStream(originProcessor.getOutputStream());
    }
    newProcessor.setEnsembleSize(originProcessor.getEnsembleSize());
    newProcessor.setEnsembleResetDecider(originProcessor.ensembleResetDecider);
    newProcessor.setDistributorStream(originProcessor.getDistributorStream());
    return newProcessor;
  }

  public Stream getDistributorStream() {
    return this.distributorStream;
  }

  public void setDistributorStream(Stream distributorStream) {
    this.distributorStream = distributorStream;
  }

  public void setEnsembleResetDecider(EnsembleResetDecider decider) {
    this.ensembleResetDecider = decider;
  }

  private void processSelection(SelectionContentEvent event) throws IllegalAccessException {
    Selection selection = event.getSelection();
    Collection<Integer> ensembleIdsToReset = ensembleResetDecider.getLearnerIdsToReset();
    if (ensembleIdsToReset.isEmpty())
      return;
    //send new information to reset ensemble members in distributor
    distributorStream.put(new SelectFeaturesContentEvent(ensembleIdsToReset, selection));
    ensembleResetDecider.resetLearners(ensembleIdsToReset);
  }
}
