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

import mockit.*;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.InstanceContentEvent;
import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilterCreator;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorker;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.LocalLearnerCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.EvaluationWorkerCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethodCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.events.DriftContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationResultContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class EvaluatorNodeProcessorTest {
  @Mocked
  private Stream stream;
  @Mocked
  private EvaluationWorkerRepository evaluationWorkerRepository;
  @Mocked
  private LocalLearnerCreator localLearnerCreator;
  @Mocked
  private FeatureFilterCreator featureFilterCreator;
  @Mocked
  private EvaluationMethodCreator evaluationMethodCreator;
  @Mocked
  private EvaluationWorkerCreator evaluationWorkerCreator;

  private final int evaluationPeriod = 2;

  @Tested
  private EvaluatorNodeProcessor processor;

  @Before
  public void setUp() {
    processor = new EvaluatorNodeProcessor.Builder()
        .evaluationPeriod(evaluationPeriod)
        .localLearnerCreator(localLearnerCreator)
        .featureFilterCreator(featureFilterCreator)
        .evaluationMethodCreator(evaluationMethodCreator)
        .evaluationWorkerCreator(evaluationWorkerCreator)
        .evaluationWorkerRepository(evaluationWorkerRepository)
        .build();
    processor.setOutputStream(stream);
  }

  @Test
  public void testThatStartNewEvaluationsForNormalUseCaseWorks(@Mocked final EvaluationContentEvent event,
      @Mocked final FeatureFilter filter,
      @Mocked final Selection selection,
      @Mocked final EvaluationMethod method,
      @Mocked final LocalLearner learner,
      @Mocked final EvaluationWorker worker)
      throws InstantiationException, IllegalAccessException {

    new Expectations() {
      {
        event.getSelection();
        result = selection;
        event.getEvaluationId();
        returns(1, 2);
        localLearnerCreator.getLearner();
        result = learner;
        featureFilterCreator.getFilter();
        result = filter;
        evaluationMethodCreator.getMethod();
        result = method;
        evaluationWorkerCreator.getWorker(learner, filter, method, evaluationPeriod);
        result = worker;
      }
    };

    processor.process(event);
    processor.process(event);

    new FullVerifications() {
      {
        filter.setSelection(selection);
        times = 2;
        event.getEvaluationId();
        times = 2;
        localLearnerCreator.getLearner();
        times = 2;
        featureFilterCreator.getFilter();
        times = 2;
        evaluationMethodCreator.getMethod();
        times = 2;
        evaluationWorkerCreator.getWorker(learner, filter, method, evaluationPeriod);
        times = 2;
        evaluationWorkerRepository.addWorker(1, worker);
        times = 1;
        evaluationWorkerRepository.addWorker(2, worker);
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessInstanceWithoutNewEvaluationResultEventsWorks(@Mocked final InstanceContentEvent event,
      @Mocked final Instance instance) {
    new Expectations() {
      {
        event.getInstance();
        result = instance;
        event.isTraining();
        result = true;
        event.isTesting();
        result = true;
      }
    };

    processor.process(event);

    new FullVerifications() {
      {
        evaluationWorkerRepository.processInstance(instance, true, true);
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessInstanceWithNewEvaluationResultEventsWorks(
      @Mocked final InstanceContentEvent instanceEvent,
      @Mocked final Instance instance,
      @Mocked final EvaluationResultContentEvent resultEvent) {

    new Expectations() {
      {
        instanceEvent.getInstance();
        result = instance;
        instanceEvent.isTesting();
        result = true;
        instanceEvent.isTraining();
        result = true;
        evaluationWorkerRepository.processInstance(instance, true, true);
        returns(Arrays.asList(), Arrays.asList(resultEvent));
      }
    };

    processor.process(instanceEvent);
    processor.process(instanceEvent);

    new FullVerifications() {
      {
        stream.put(resultEvent);
      }
    };
  }

  @Test
  public void testThatDriftSignalIsHandled(@Mocked final DriftContentEvent event) {
    processor.process(event);

    new Verifications() {
      {
        evaluationWorkerRepository.purge();
        times = 1;
      }
    };
  }
}
