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
import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.InstanceContentEvent;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilter;
import org.apache.samoa.preprocessing.featureselection.ranking.FeatureFilterCreator;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class DynamicFeatureDistributorProcessorTest {
  @Mocked
  Stream featureSelectionStream;
  @Mocked
  Stream ensembleMember1Stream;
  @Mocked
  Stream ensembleMember2Stream;
  @Mocked
  Stream ensembleMember3Stream;

  @Mocked
  FeatureFilterCreator featureFilterCreator;

  @Tested
  private DynamicFeatureDistributorProcessor processor;

  @Before
  public void setUp() {
    processor = new DynamicFeatureDistributorProcessor();
    processor.setFeatureFilterCreator(featureFilterCreator);
    processor.setFeatureSelectionStream(featureSelectionStream);
    processor.setOutputStreams(new Stream[]{ensembleMember1Stream, ensembleMember2Stream, ensembleMember3Stream});
  }

  @Test
  public void testThatProcessSelectionEventForNormalUseCaseWorks(@Mocked final SelectionContentEvent event) {
    processor.process(event);
    new FullVerifications() {
      {
        featureSelectionStream.put(event);
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessSelectFeaturesEventForNormalUseCaseWorks(@Mocked final SelectFeaturesContentEvent event,
      @Mocked final FeatureFilter filter) {
    new Expectations() {
      {
        event.getEnsembleStreamIds();
        result = Arrays.asList(0, 1, 2);
        featureFilterCreator.getFilter();
        result = filter;
      }
    };

    processor.process(event);

    new Verifications() {
      {
        event.getEnsembleStreamIds();
        times = 1;
        featureFilterCreator.getFilter();
        times = 3;
        event.getSelection();
        times = 3;

      }
    };
  }

  @Test
  public void testThatProcessInstanceEventWithExistingFiltersWorks(@Mocked final InstanceContentEvent instanceEvent,
      @Mocked final SelectFeaturesContentEvent filterEvent,
      @Mocked final FeatureFilter filter,
      @Mocked final Stream ensembleStream,
      @Mocked final Instance instance,
      @Mocked final Instance instanceCopy,
      @Mocked final Instance instanceFiltered) {

    Stream[] ensembleStreams = new Stream[] { ensembleStream, ensembleStream, ensembleStream };
    int ensembleSize = 3;
    processor.setOutputStreams(ensembleStreams);
    processor.setEnsembleSize(ensembleSize);

    new Expectations() {
      {
        filterEvent.getEnsembleStreamIds();
        result = Arrays.asList(0, 1);
        featureFilterCreator.getFilter();
        result = filter;

        instanceEvent.isTesting();
        result = true;
        instanceEvent.getInstance();
        result = instance;
        instance.copy();
        result = instanceCopy;
        filter.processInstance(instance);
        result = instanceFiltered;
      }
    };

    processor.process(filterEvent);
    processor.process(instanceEvent);

    new Verifications() {
      {
        filterEvent.getEnsembleStreamIds();
        times = 1;
        featureFilterCreator.getFilter();
        times = 2;
        filterEvent.getSelection();
        times = 2;

        filter.processInstance(instance);
        times = 2;
        instance.copy();
        times = 1;
        ensembleStream.put(withInstanceOf(InstanceContentEvent.class));
        times = 3;
      }
    };
  }
}