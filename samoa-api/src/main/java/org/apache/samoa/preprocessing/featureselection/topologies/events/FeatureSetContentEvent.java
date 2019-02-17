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
package org.apache.samoa.preprocessing.featureselection.topologies.events;

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
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;

/**
 * Partial instance information used to distribute ranking computations
 */
public class FeatureSetContentEvent implements ContentEvent {
  private static final long serialVersionUID = 0L;
  private final transient String key;
  private final int rankingIdentifier;
  private final FeatureSet nominalSet;
  private final FeatureSet numericSet;

  /**
   * Create FeatureSetContentEvent instance
   * 
   * @param nominalSet
   *          Information about nominal attributes
   * @param numericSet
   *          Information about numeric attributes
   * @param rankingIdentifier
   *          Identifier of designated ranking
   * @param setIdentifier
   *          Set identifier, used to pass appropriate attributes to appropriate ranker node. Corresponds to splitter
   *          work - which splits instance into few parts with unique set identifiers.
   */
  public FeatureSetContentEvent(FeatureSet nominalSet, FeatureSet numericSet, int rankingIdentifier,
      int setIdentifier) {
    this.nominalSet = nominalSet;
    this.numericSet = numericSet;
    this.rankingIdentifier = rankingIdentifier;
    this.key = String.format("%d-%d", setIdentifier, this.rankingIdentifier);
  }

  public FeatureSet getNominalSet() {
    return nominalSet;
  }

  public FeatureSet getNumericSet() {
    return numericSet;
  }

  public int getRankingIdentifier() {
    return rankingIdentifier;
  }

  @Override
  public String getKey() {
    return this.key;
  }

  @Override
  public void setKey(String key) {
  }

  @Override
  public boolean isLastEvent() {
    return false;
  }
}
