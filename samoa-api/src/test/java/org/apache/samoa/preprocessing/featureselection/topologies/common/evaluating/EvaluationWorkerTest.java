package org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating;

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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EvaluationWorkerTest {

  @Mocked
  private FeatureFilter filter;
  @Mocked
  private LocalLearner learner;
  @Mocked
  private EvaluationMethod method;
  @Mocked
  private Instance instance;
  @Mocked
  private Instance filteredInstance;

  @Tested
  private EvaluationWorker worker;

  @Before
  public void setUp() {
    worker = new EvaluationWorker(learner, filter, method, 2);
  }

  @Test
  public void testThatProcessInstanceForNormalUseCaseWorks() {
    new Expectations() {
      {
        filter.processInstance(instance);
        result = filteredInstance;
        learner.getVotesForInstance(filteredInstance);
        result = new double[] { 0.5d, 1d };
      }
    };

    boolean resultFirstInstance = worker.processInstance(instance, true, true);
    boolean resultSecondInstance = worker.processInstance(instance, false, true);

    assertFalse(resultFirstInstance);
    assertTrue(resultSecondInstance);
    new Verifications() {
      {
        filter.processInstance(instance);
        times = 2;
        double[] votes = new double[2];
        method.update(votes = withCapture(), filteredInstance);
        times = 1;
        assertEquals(0.5d, votes[0], Double.MIN_VALUE);
        assertEquals(1d, votes[1], Double.MIN_VALUE);
        learner.trainOnInstance(filteredInstance);
        times = 2;
      }
    };
  }
}
