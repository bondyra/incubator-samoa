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

import mockit.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.learners.ResultContentEvent;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicFeaturePredictionCombinerProcessorTest {
  @Mocked
  private Stream distributorStream;
  @Mocked
  private EnsembleResetDecider ensembleResetDecider;

  @Tested
  private DynamicFeaturePredictionCombinerProcessor processor;

  @Before
  public void setUp() {
    processor = new DynamicFeaturePredictionCombinerProcessor();
    processor.setEnsembleResetDecider(ensembleResetDecider);
    processor.setDistributorStream(distributorStream);
    processor.setEnsembleSize(3);
  }

  @Test
  public void testThatProcessResultEventForNormalUseCaseWorks(@Mocked final ResultContentEvent event)
      throws InstantiationException {

    processor.process(event);

    new Verifications() {
      {
        ensembleResetDecider.processResultContentEvent(event);
        times = 1;
        distributorStream.put(withInstanceOf(SelectFeaturesContentEvent.class));
        times = 0;
      }
    };
  }

  @Test
  public void testThatProcessWithNewResetRequestWorks(@Mocked final SelectionContentEvent event,
      @Mocked final Selection selection) throws IllegalAccessException {
    new Expectations() {
      {
        event.getSelection();
        result = selection;
        ensembleResetDecider.getLearnerIdsToReset();
        result = Arrays.asList(0, 1);
      }
    };

    processor.process(event);

    new FullVerifications() {
      {
        ensembleResetDecider.getLearnerIdsToReset();
        times = 1;
        Collection<Integer> learnerIdsToReset;
        ensembleResetDecider.resetLearners(learnerIdsToReset = withCapture());
        assertTrue(CollectionUtils.isEqualCollection(learnerIdsToReset, Arrays.asList(0, 1)));
        SelectFeaturesContentEvent filterEvent;
        distributorStream.put(filterEvent = withCapture());
        assertTrue(CollectionUtils.isEqualCollection(learnerIdsToReset, filterEvent.getEnsembleStreamIds()));
        assertEquals(selection, filterEvent.getSelection());
      }
    };
  }
}