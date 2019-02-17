package org.apache.samoa.preprocessing.featureselection.topologies.common.deciders;

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
import mockit.Tested;
import mockit.Verifications;
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating.Evaluation;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicEvaluationDeciderTest {
  @Mocked
  private Stream stream;

  @Tested
  private BasicEvaluationDecider decider;

  @Before
  public void setUp() {
    decider = new BasicEvaluationDecider();
    decider.evaluationDecisionBoundaryOption.setValue(0.02d);
    decider.setDecisionStream(stream);
  }

  @Test
  public void testThatSingleProcessEvaluationResultsWhenFirstEvaluationIsBetterWorks() {
    Evaluation evaluation1 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    Evaluation evaluation2 = new Evaluation(new Selection(Arrays.asList(1, 2, 3)));
    evaluation1.setEvaluationResult(1d);
    evaluation1.setEvaluationUpperBound(2d); // normalized result would be 0.5
    evaluation2.setEvaluationResult(0.3d);
    evaluation2.setEvaluationUpperBound(1.5d); // normalized result would be 0.2 (below difference - 0.2)

    Collection<Evaluation> evaluations = Arrays.asList(evaluation1, evaluation2);
    decider.processEvaluationResults(evaluations);

    new Verifications() {
      {
        SelectionContentEvent event;

        stream.put(event = withCapture());
        times = 1;

        assertTrue(
            CollectionUtils.isEqualCollection(event.getSelection().getSelectedIndexes(), Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatSingleProcessEvaluationResultsWhenSecondEvaluationIsBetterWorks() {
    Evaluation evaluation1 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    Evaluation evaluation2 = new Evaluation(new Selection(Arrays.asList(1, 2, 3)));
    evaluation1.setEvaluationResult(1d);
    evaluation1.setEvaluationUpperBound(2d); // normalized result would be 0.5
    evaluation2.setEvaluationResult(1.215d);
    evaluation2.setEvaluationUpperBound(1.5d); // normalized result would be 0.71 (above difference - 0.2)

    Collection<Evaluation> evaluations = Arrays.asList(evaluation1, evaluation2);
    decider.processEvaluationResults(evaluations);

    new Verifications() {
      {
        SelectionContentEvent event;

        stream.put(event = withCapture());
        times = 1;

        assertTrue(
            CollectionUtils.isEqualCollection(event.getSelection().getSelectedIndexes(), Arrays.asList(1, 2, 3)));
      }
    };
  }

  @Test
  public void testThatMultipleProcessEvaluationResultsWhenSecondProcessHasBetterEvaluationWorks() {
    Evaluation evaluation1 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    evaluation1.setEvaluationResult(0.7d);
    evaluation1.setEvaluationUpperBound(2d); // normalized result would be 0.35
    Collection<Evaluation> evaluations1 = Collections.singletonList(evaluation1);

    Evaluation evaluation2 = new Evaluation(new Selection(Arrays.asList(1, 2, 3)));
    evaluation2.setEvaluationResult(1d);
    evaluation2.setEvaluationUpperBound(1d); // normalized result would be 1
    Collection<Evaluation> evaluations2 = Arrays.asList(evaluation1, evaluation2);

    decider.processEvaluationResults(evaluations1);
    decider.processEvaluationResults(evaluations2);

    new Verifications() {
      {
        List<SelectionContentEvent> events = new ArrayList<>();

        stream.put(withCapture(events));
        assertEquals(2, events.size()); // two decisions should be put to stream
        assertTrue(CollectionUtils.isEqualCollection(events.get(0).getSelection().getSelectedIndexes(),
            Arrays.asList(0, 1, 2)));
        assertTrue(CollectionUtils.isEqualCollection(events.get(1).getSelection().getSelectedIndexes(),
            Arrays.asList(1, 2, 3)));
      }
    };
  }

  @Test
  public void testThatMultipleProcessEvaluationResultsWhenSecondProcessHasSimilarEvaluationWorks() {
    Evaluation evaluation1 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    evaluation1.setEvaluationResult(0.7d);
    evaluation1.setEvaluationUpperBound(1d); // normalized result would be 0.7
    Collection<Evaluation> evaluations1 = Collections.singletonList(evaluation1);

    Evaluation evaluation2 = new Evaluation(new Selection(Arrays.asList(1, 2)));
    evaluation2.setEvaluationResult(1.42d);
    evaluation2.setEvaluationUpperBound(2d); // normalized result would be 0.71
    Collection<Evaluation> evaluations2 = Arrays.asList(evaluation1, evaluation2);

    decider.processEvaluationResults(evaluations1);
    decider.processEvaluationResults(evaluations2);

    new Verifications() {
      {
        List<SelectionContentEvent> events = new ArrayList<>();

        stream.put(withCapture(events));
        assertEquals(1, events.size()); // only one decision is put to stream
        assertTrue(CollectionUtils.isEqualCollection(events.get(0).getSelection().getSelectedIndexes(),
            Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatMultipleProcessEvaluationResultsWhenSecondProcessHasWorseEvaluationWorks() {
    Evaluation evaluation1 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    evaluation1.setEvaluationResult(0.7d);
    evaluation1.setEvaluationUpperBound(1d); // normalized result would be 0.7
    Collection<Evaluation> evaluations1 = Collections.singletonList(evaluation1);

    Evaluation evaluation2 = new Evaluation(new Selection(Arrays.asList(1, 2, 3)));
    evaluation2.setEvaluationResult(0.98d);
    evaluation2.setEvaluationUpperBound(2d); // normalized result would be 0.49
    Collection<Evaluation> evaluations2 = Arrays.asList(evaluation1, evaluation2);

    decider.processEvaluationResults(evaluations1);
    decider.processEvaluationResults(evaluations2);

    new Verifications() {
      {
        List<SelectionContentEvent> events = new ArrayList<>();

        stream.put(withCapture(events));
        assertEquals(1, events.size()); // only one decision is put to stream
        assertTrue(CollectionUtils.isEqualCollection(events.get(0).getSelection().getSelectedIndexes(),
            Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatMultipleProcessEvaluationResultsWithSameEvaluationsWorks() {
    // when evaluation is set up again, but the selection remains exactly the same,
    // no additional stream put should be invoked
    Evaluation evaluation1 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    evaluation1.setEvaluationResult(0.7d);
    evaluation1.setEvaluationUpperBound(1d); // normalized result would be 0.7
    Collection<Evaluation> evaluations1 = Collections.singletonList(evaluation1);

    Evaluation evaluation2 = new Evaluation(new Selection(Arrays.asList(0, 1, 2)));
    evaluation2.setEvaluationResult(2d);
    evaluation2.setEvaluationUpperBound(2d); // normalized result would be 1.0
    Collection<Evaluation> evaluations2 = Arrays.asList(evaluation1, evaluation2);

    decider.processEvaluationResults(evaluations1);
    decider.processEvaluationResults(evaluations2);

    new Verifications() {
      {
        List<SelectionContentEvent> events = new ArrayList<>();

        stream.put(withCapture(events));
        assertEquals(1, events.size()); // only one decision is put to stream
        assertTrue(CollectionUtils.isEqualCollection(events.get(0).getSelection().getSelectedIndexes(),
            Arrays.asList(0, 1, 2)));
      }
    };
  }
}
