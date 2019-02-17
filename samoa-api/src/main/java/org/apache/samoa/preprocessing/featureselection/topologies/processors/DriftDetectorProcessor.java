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

import org.apache.samoa.core.Processor;
import org.apache.samoa.learners.classifiers.LocalLearnerProcessor;
import org.apache.samoa.preprocessing.featureselection.topologies.events.DriftContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.WarningContentEvent;
import org.apache.samoa.topology.Stream;

/**
 * DriftDetectorProcessor. Based on local learner processor, it provides adapation for feature selection systems Uses
 * local model and drift detector, that puts necessary events to output stream
 */
public class DriftDetectorProcessor extends LocalLearnerProcessor {
  private Stream driftInfoStream;

  /**
   * Throttles drift signals to ensure better stability of feature selection
   */
  private int delayBetweenDrifts;
  private int updatesSinceLastDrift = 0;
  private boolean driftOccured = false;

  public void setDriftInfoStream(Stream outputStream) {
    this.driftInfoStream = outputStream;
  }

  public Stream getDriftInfoStream() {
    return driftInfoStream;
  }

  @Override
  protected void updateChangeDetector(boolean correctlyClassifies) {
    this.updatesSinceLastDrift++;
    double oldEstimation = this.changeDetector.getEstimation();
    this.changeDetector.input(correctlyClassifies ? 0 : 1);
    if (this.changeDetector.getWarningZone()) {
      this.driftInfoStream.put(new WarningContentEvent());
    }
    if ((this.changeDetector.getChange() && this.changeDetector.getEstimation() > oldEstimation)) {
      if (!this.driftOccured || this.updatesSinceLastDrift >= this.delayBetweenDrifts) {
        // put drift signal only if enough observations were observed since last one
        this.driftInfoStream.put(new DriftContentEvent());
        this.updatesSinceLastDrift = 0;
      }
      this.driftOccured = true;
      // start a new classifier
      this.model.resetLearning();
      this.changeDetector.resetLearning();
    }
  }

  @Override
  public Processor newProcessor(Processor sourceProcessor) {
    DriftDetectorProcessor newProcessor = new DriftDetectorProcessor();
    DriftDetectorProcessor originProcessor = (DriftDetectorProcessor) sourceProcessor;

    if (originProcessor.getLearner() != null) {
      newProcessor.setLearner(originProcessor.getLearner().create());
    }

    if (originProcessor.getChangeDetector() != null) {
      newProcessor.setChangeDetector(originProcessor.getChangeDetector());
    }

    newProcessor.setDelayBetweenDrifts(originProcessor.delayBetweenDrifts);
    newProcessor.setOutputStream(originProcessor.getOutputStream());
    newProcessor.setDriftInfoStream(originProcessor.getDriftInfoStream());
    return newProcessor;
  }

  public void setDelayBetweenDrifts(int delayBetweenDrifts) {
    this.delayBetweenDrifts = delayBetweenDrifts;
  }
}
