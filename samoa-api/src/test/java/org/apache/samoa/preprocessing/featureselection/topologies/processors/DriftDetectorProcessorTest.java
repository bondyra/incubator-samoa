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

import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.learners.InstanceContentEvent;
import org.apache.samoa.learners.classifiers.LocalLearner;
import org.apache.samoa.moa.classifiers.core.driftdetection.ChangeDetector;
import org.apache.samoa.preprocessing.featureselection.topologies.events.DriftContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.WarningContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

public class DriftDetectorProcessorTest {
  @Mocked
  private Stream outputStream;
  @Mocked
  private Stream driftInfoStream;
  @Mocked
  private ChangeDetector changeDetector;
  @Mocked
  private LocalLearner learner;

  @Tested
  private DriftDetectorProcessor processor;

  @Before
  public void setUp() {
    processor = new DriftDetectorProcessor();
    processor.setDriftInfoStream(driftInfoStream);
    processor.setOutputStream(outputStream);
    processor.setLearner(learner);
    processor.setChangeDetector(changeDetector);
  }

  @Test
  public void testThatWarningSignalIsHandled(@Mocked final InstanceContentEvent event,
      @Mocked final Instance instance) {
    new Expectations() {
      {
        event.getInstance();
        result = instance;
        event.isTraining();
        result = true;
        changeDetector.getWarningZone();
        result = true;
      }
    };

    processor.process(event);

    new Verifications() {
      {
        learner.trainOnInstance(instance);
        times = 1;
        driftInfoStream.put(withInstanceOf(WarningContentEvent.class));
        times = 1;
      }
    };
  }

  @Test
  public void testThatDriftSignalIsHandled(@Mocked final InstanceContentEvent event, @Mocked final Instance instance) {
    new Expectations() {
      {
        event.getInstance();
        result = instance;
        event.isTraining();
        result = true;
        changeDetector.getWarningZone();
        result = false;
        changeDetector.getChange();
        result = true;
        changeDetector.getEstimation();
        returns(0.5d, 1d);
      }
    };

    processor.process(event);

    new Verifications() {
      {
        learner.trainOnInstance(instance);
        times = 1;
        driftInfoStream.put(withInstanceOf(DriftContentEvent.class));
        times = 1;
        learner.resetLearning();
        times = 1;
        changeDetector.resetLearning();
        times = 1;
      }
    };
  }
}
