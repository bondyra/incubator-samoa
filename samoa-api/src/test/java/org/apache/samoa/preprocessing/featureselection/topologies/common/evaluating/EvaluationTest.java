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

import mockit.Mocked;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EvaluationTest {

  @Mocked
  private Selection selection;

  @Test(expected = IllegalStateException.class)
  public void testThatGetEvaluationResultWhenEvaluationIsNotPreparedRaisesException() {
    Evaluation evaluation = new Evaluation(selection);

    evaluation.getEvaluationResult();
  }

  @Test
  public void testThatGetEvaluationResultWhenEvaluationIsPreparedWorks() {
    Evaluation evaluation = new Evaluation(selection);

    evaluation.setEvaluationResult(1d);
    double result = evaluation.getEvaluationResult();

    assertEquals(1d, result, Double.MIN_VALUE);
  }
}
