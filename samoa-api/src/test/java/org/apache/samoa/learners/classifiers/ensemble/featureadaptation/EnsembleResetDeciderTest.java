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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.apache.samoa.core.SerializableInstance;
import org.apache.samoa.learners.ResultContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethod;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.methods.EvaluationMethodCreator;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EnsembleResetDeciderTest {
  @Mocked
  private EvaluationMethodCreator evaluationMethodCreator;
  @Tested
  private EnsembleResetDecider ensembleResetDecider;

  @Before
  public void setUp() {
    ensembleResetDecider = new EnsembleResetDecider(2, 1);
    ensembleResetDecider.setMethodCreator(evaluationMethodCreator);
  }

  @Test
  public void testThatGetResetProbabilityWorks(@Mocked final Stream ensembleStream) {
    // edge cases
    double probabilityForOnlyLearner = EnsembleResetDecider.getResetProbability(0, 1);
    double probabilityWorstLearner = EnsembleResetDecider.getResetProbability(0, 10);
    double probabilityBestLearner1 = EnsembleResetDecider.getResetProbability(1, 2);
    double probabilityBestLearner2 = EnsembleResetDecider.getResetProbability(9, 10);
    // check if intermediate results are linear
    double probabilityMiddleLearner1 = EnsembleResetDecider.getResetProbability(1, 3);
    double probabilityMiddleLearner2 = EnsembleResetDecider.getResetProbability(1, 6);
    double probabilityMiddleLearner3 = EnsembleResetDecider.getResetProbability(2, 6);
    double probabilityMiddleLearner4 = EnsembleResetDecider.getResetProbability(3, 6);
    double probabilityMiddleLearner5 = EnsembleResetDecider.getResetProbability(4, 6);

    assertEquals(1d, probabilityForOnlyLearner, Double.MIN_VALUE);
    assertEquals(1d, probabilityWorstLearner, Double.MIN_VALUE);
    assertEquals(0d, probabilityBestLearner1, Double.MIN_VALUE);
    assertEquals(0d, probabilityBestLearner2, Double.MIN_VALUE);

    assertEquals(0.5d, probabilityMiddleLearner1, Double.MIN_VALUE);
    assertEquals(0.8d, probabilityMiddleLearner2, Double.MIN_VALUE);
    assertEquals(0.6d, probabilityMiddleLearner3, Double.MIN_VALUE);
    assertEquals(0.4d, probabilityMiddleLearner4, Double.MIN_VALUE);
    assertEquals(0.2d, probabilityMiddleLearner5, Double.MIN_VALUE);
  }

  @Test
  public void testProcessResultContentEvent(@Mocked final ResultContentEvent event,
      @Mocked final EvaluationMethod method1,
      @Mocked final EvaluationMethod method2,
      @Mocked final double[] votes,
      @Mocked final SerializableInstance instance) throws InstantiationException {
    new Expectations() {
      {
        event.getClassifierIndex();
        returns(1, 2);
        evaluationMethodCreator.getMethod();
        returns(method1, method2);
        event.getClassVotes();
        result = votes;
        event.getInstance();
        result = instance;
      }
    };

    ensembleResetDecider.processResultContentEvent(event);
    ensembleResetDecider.processResultContentEvent(event);

    new Verifications() {
      {
        method1.update(votes, instance);
        times = 1;
        method2.update(votes, instance);
        times = 1;
      }
    };
  }

  @Test
  public void testThatGetLearnerIdsToResetWorks(@Mocked final ResultContentEvent event,
      @Mocked final EvaluationMethod method1,
      @Mocked final EvaluationMethod method2,
      @Mocked final double[] votes,
      @Mocked final SerializableInstance instance) throws InstantiationException {
    new Expectations() {
      {
        event.getClassifierIndex();
        returns(1, 2);
        evaluationMethodCreator.getMethod();
        returns(method1, method2);
        event.getClassVotes();
        result = votes;
        event.getInstance();
        result = instance;
        method1.getScore();
        result = 1d;
        method2.getScore();
        result = 0.2d;
      }
    };

    ensembleResetDecider.processResultContentEvent(event);
    ensembleResetDecider.processResultContentEvent(event);
    Collection<Integer> learnerIdsToReset = ensembleResetDecider.getLearnerIdsToReset();
    assertTrue(learnerIdsToReset.size() > 0); // no point in going further, as results are non-deterministic
    assertTrue(learnerIdsToReset.contains(2)); // worst learner should be reset at all times
    new Verifications() {
      {
        method1.update(votes, instance);
        times = 1;
        method2.update(votes, instance);

        method1.getScore();
        times = 1;
        method2.getScore();
        times = 1;
      }
    };
  }

  @Test
  public void testThatResetLearnersWithExistingLearnersWorks(@Mocked final ResultContentEvent event,
      @Mocked final EvaluationMethod method1,
      @Mocked final EvaluationMethod method2)
      throws InstantiationException, IllegalAccessException {
    new Expectations() {
      {
        event.getClassifierIndex();
        returns(1, 2);
        evaluationMethodCreator.getMethod();
        returns(method1, method2);
      }
    };

    ensembleResetDecider.processResultContentEvent(event);
    ensembleResetDecider.processResultContentEvent(event);
    ensembleResetDecider.resetLearners(Collections.singletonList(1));

    new Verifications() {
      {
        method1.reset();
        times = 1;
        method2.reset();
        times = 0;
      }
    };
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatResetLearnersWithNotExistingLearnersRaisesException(@Mocked final ResultContentEvent event,
      @Mocked final EvaluationMethod method1,
      @Mocked final EvaluationMethod method2) throws InstantiationException, IllegalAccessException {
    new Expectations() {
      {
        event.getClassifierIndex();
        returns(1, 2);
        evaluationMethodCreator.getMethod();
        returns(method1, method2);
      }
    };

    ensembleResetDecider.processResultContentEvent(event);
    ensembleResetDecider.processResultContentEvent(event);
    ensembleResetDecider.resetLearners(Arrays.asList(1, 3));
  }
}