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

import org.apache.samoa.core.ContentEvent;
import org.apache.samoa.core.Processor;
import org.apache.samoa.preprocessing.featureselection.topologies.common.filtering.RankingWorkerRepository;
import org.apache.samoa.preprocessing.featureselection.topologies.events.DriftContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.FeatureSetContentEvent;
import org.apache.samoa.preprocessing.featureselection.topologies.events.WarningContentEvent;
import org.apache.samoa.topology.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RankerNodeProcessor. Provides ranking computation capabilities Accepts new observations (possibly partial) and updates
 * the rankings Sends occasionally computed ranking messages
 */
public class RankerNodeProcessor implements Processor {

  private static final long serialVersionUID = 0L;

  private RankingWorkerRepository rankingWorkerRepository;

  private Stream outputStream;

  private static final Logger logger = LoggerFactory.getLogger(RankerNodeProcessor.class);

  public RankerNodeProcessor(Builder builder) {
    this.rankingWorkerRepository = builder.rankingWorkerRepository;
  }

  public void setOutputStream(Stream stream) {
    this.outputStream = stream;
  }

  public Stream getOutputStream() {
    return outputStream;
  }

  @Override
  public boolean process(ContentEvent event) {
    try {
      if (event instanceof FeatureSetContentEvent) {
        processFeatureSetContentEvent((FeatureSetContentEvent) event);
      } else if (event instanceof DriftContentEvent) {
        rankingWorkerRepository.processDrift();
      } else if (event instanceof WarningContentEvent) {
        rankingWorkerRepository.processWarning();
      }
    } catch (Exception e) {
      logger.error("Exception during event processing. " + e.getMessage());
    }
    return false;
  }

  /**
   * Feed partial instance information into rankers. if they have computed results, pass them
   * 
   * @param event
   *          Parial instance information
   * @throws IllegalAccessException
   * @throws IllegalStateException
   */
  private void processFeatureSetContentEvent(FeatureSetContentEvent event)
      throws IllegalAccessException, IllegalStateException {
    int rankingId = event.getRankingIdentifier();
    if (rankingWorkerRepository.processFeatureSets(rankingId, event.getNumericSet(), event.getNominalSet()))
      this.outputStream.put(rankingWorkerRepository.getEvent(rankingId));
  }

  @Override
  public void onCreate(int id) {

  }

  @Override
  public Processor newProcessor(Processor processor) {
    RankerNodeProcessor oldProcessor = (RankerNodeProcessor) processor;
    RankerNodeProcessor newProcessor = new RankerNodeProcessor.Builder(oldProcessor).build();

    newProcessor.setOutputStream(oldProcessor.outputStream);
    return newProcessor;
  }

  /**
   * Builder of RankerNodeProcessor that handles all configurable parameters
   */
  public static class Builder {
    private RankingWorkerRepository rankingWorkerRepository;

    public Builder() {
    }

    public Builder(RankerNodeProcessor oldProcessor) {
      this.rankingWorkerRepository = oldProcessor.rankingWorkerRepository;
    }

    public Builder rankingWorkerRepository(RankingWorkerRepository rankingWorkerRepository) {
      this.rankingWorkerRepository = rankingWorkerRepository;
      return this;
    }

    public RankerNodeProcessor build() {
      return new RankerNodeProcessor(this);
    }
  }
}
