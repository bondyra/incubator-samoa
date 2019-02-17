/*
 * Copyright 2018 The Apache Software Foundation.
 *
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
 */
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
import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilterCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.LocalLearnerCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorkerCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethodCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.events.DriftContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationResultContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorker;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * EvaluatorNodeProcessor. Provides wrapper method capabilities Acquires requests of evaluation and sends result after
 * they are done
 */
public class EvaluatorNodeProcessor implements Processor {

  private static final long serialVersionUID = 0L;

  // creators for dynamic evaluator creation
  private LocalLearnerCreator localLearnerCreator;
  private FeatureFilterCreator featureFilterCreator;
  private EvaluationMethodCreator evaluationMethodCreator;
  private EvaluationWorkerCreator evaluationWorkerCreator;

  private EvaluationWorkerRepository evaluationWorkerRepository;

  private Stream outputStream;

  /**
   * Indicates for how any observations evaluation should last
   */
  private int evaluationPeriod;

  private static final Logger logger = LoggerFactory.getLogger(EvaluatorNodeProcessor.class);

  private EvaluatorNodeProcessor(Builder builder) {
    this.evaluationPeriod = builder.evaluationPeriod;
    this.localLearnerCreator = builder.localLearnerCreator;
    this.featureFilterCreator = builder.featureFilterCreator;
    this.evaluationMethodCreator = builder.evaluationMethodCreator;
    this.evaluationWorkerCreator = builder.evaluationWorkerCreator;
    this.evaluationWorkerRepository = builder.evaluationWorkerRepository;
  }

  public void setOutputStream(Stream stream) {
    this.outputStream = stream;
  }

  public Stream getOutputStream() {
    return this.outputStream;
  }

  @Override
  public boolean process(ContentEvent event) {
    try {
      if (event instanceof InstanceContentEvent) {
        this.processInstance((InstanceContentEvent) event);
      } else if (event instanceof EvaluationContentEvent) {
        this.startNewEvaluation((EvaluationContentEvent) event);
      } else if (event instanceof DriftContentEvent) {
        // naive drift handling method - resign from all evaluations
        this.evaluationWorkerRepository.purge();
      }
    } catch (Exception e) {
      logger.error("Exception during event processing. " + e.toString());
    }
    return false;
  }

  /**
   * When a new instance arrives, pass it to all current evaluations. pass results if they are available
   * 
   * @param event
   *          instance information
   */
  private void processInstance(InstanceContentEvent event) {
    Collection<EvaluationResultContentEvent> eventsToSend = evaluationWorkerRepository
        .processInstance(event.getInstance(), event.isTesting(), event.isTraining());
    for (ContentEvent eventToSend : eventsToSend) {
      outputStream.put(eventToSend);
    }
  }

  /**
   * Dynamically create new entry in evaluation repository
   * 
   * @param event
   *          Evaluation request
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  private void startNewEvaluation(EvaluationContentEvent event)
      throws InstantiationException, IllegalAccessException {
    int evaluationId = event.getEvaluationId();
    Selection selection = event.getSelection();
    FeatureFilter filter = featureFilterCreator.getFilter();
    filter.setSelection(selection);
    LocalLearner learner = localLearnerCreator.getLearner();
    EvaluationMethod method = evaluationMethodCreator.getMethod();
    EvaluationWorker newWorker = evaluationWorkerCreator.getWorker(learner, filter, method, evaluationPeriod);
    this.evaluationWorkerRepository.addWorker(evaluationId, newWorker);
  }

  @Override
  public void onCreate(int id) {

  }

  @Override
  public Processor newProcessor(Processor processor) {
    EvaluatorNodeProcessor oldProcessor = (EvaluatorNodeProcessor) processor;
    EvaluatorNodeProcessor newProcessor = new EvaluatorNodeProcessor.Builder(oldProcessor).build();

    newProcessor.setOutputStream(oldProcessor.outputStream);
    return newProcessor;
  }

  /**
   * Builder of EvaluatorNodeProcessor that handles all configurable parameters
   */
  public static class Builder {
    private int evaluationPeriod;
    private EvaluationWorkerRepository evaluationWorkerRepository;
    private LocalLearnerCreator localLearnerCreator;
    private FeatureFilterCreator featureFilterCreator;
    private EvaluationWorkerCreator evaluationWorkerCreator;
    private EvaluationMethodCreator evaluationMethodCreator;

    public Builder() {
    }

    public Builder(EvaluatorNodeProcessor oldProcessor) {
      this.evaluationPeriod = oldProcessor.evaluationPeriod;
      this.evaluationWorkerRepository = oldProcessor.evaluationWorkerRepository;
      this.localLearnerCreator = oldProcessor.localLearnerCreator;
      this.featureFilterCreator = oldProcessor.featureFilterCreator;
      this.evaluationMethodCreator = oldProcessor.evaluationMethodCreator;
      this.evaluationWorkerCreator = oldProcessor.evaluationWorkerCreator;
    }

    public Builder evaluationWorkerRepository(EvaluationWorkerRepository evaluationWorkerRepository) {
      this.evaluationWorkerRepository = evaluationWorkerRepository;
      return this;
    }

    public Builder localLearnerCreator(LocalLearnerCreator localLearnerCreator) {
      this.localLearnerCreator = localLearnerCreator;
      return this;
    }

    public Builder featureFilterCreator(FeatureFilterCreator featureFilterCreator) {
      this.featureFilterCreator = featureFilterCreator;
      return this;
    }

    public Builder evaluationMethodCreator(EvaluationMethodCreator evaluationMethodCreator) {
      this.evaluationMethodCreator = evaluationMethodCreator;
      return this;
    }

    public Builder evaluationWorkerCreator(EvaluationWorkerCreator evaluationWorkerCreator) {
      this.evaluationWorkerCreator = evaluationWorkerCreator;
      return this;
    }

    public Builder evaluationPeriod(int evaluationPeriod) {
      this.evaluationPeriod = evaluationPeriod;
      return this;
    }

    public EvaluatorNodeProcessor build() {
      return new EvaluatorNodeProcessor(this);
    }
  }
}
