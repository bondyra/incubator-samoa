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

import mockit.*;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.preprocessing.featureselection.topologies.events.EvaluationResultContentEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class EvaluationWorkerRepositoryTest {

  @Tested
  private EvaluationWorkerRepository repository;

  @Before
  public void setUp() {
    repository = new EvaluationWorkerRepository();
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatAddWorkerForSameWorkerAddedMultipleTimesRaisesException(@Mocked final EvaluationWorker worker)
      throws IllegalAccessException {
    repository.addWorker(0, worker);
    repository.addWorker(0, worker);
  }

  @Test
  public void testThatProcessInstanceForNormalUseCaseWorks(@Mocked final EvaluationWorker worker,
      @Mocked final Instance instance)
      throws IllegalAccessException {
    new Expectations() {
      {
        worker.processInstance(instance, true, true);
        returns(
            false, true, false);
        worker.processInstance(instance, true, false);
        returns(
            false, true);
        worker.getScore();
        returns(0.5d, 0.8d);
        worker.getUpperBound();
        returns(2d, 1d);
      }
    };

    repository.addWorker(1, worker);
    repository.addWorker(2, worker);
    repository.addWorker(3, worker);
    List<EvaluationResultContentEvent> first = repository.processInstance(instance, true, true);
    List<EvaluationResultContentEvent> second = repository.processInstance(instance, true, false);

    assertEquals(1, first.size());
    assertEquals(0.5d, first.get(0).getEvaluationResult(), Double.MIN_VALUE);
    assertEquals(2, first.get(0).getEvaluationId());
    assertEquals(2d, first.get(0).getEvaluationUpperBound(), Double.MIN_VALUE);
    assertEquals(1, second.size());
    assertEquals(0.8d, second.get(0).getEvaluationResult(), Double.MIN_VALUE);
    assertEquals(3, second.get(0).getEvaluationId());
    assertEquals(1d, second.get(0).getEvaluationUpperBound(), Double.MIN_VALUE);

    new FullVerifications() {
      {
        worker.processInstance(instance, true, true);
        times = 3;
        worker.processInstance(instance, true, false);
        times = 2;
        worker.getScore();
        times = 2;
        worker.getUpperBound();
        times = 2;
      }
    };
  }
}
