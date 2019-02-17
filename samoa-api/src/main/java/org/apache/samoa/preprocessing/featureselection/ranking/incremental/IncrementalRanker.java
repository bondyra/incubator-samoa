package org.apache.samoa.preprocessing.featureselection.ranking.incremental;

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

import org.apache.samoa.preprocessing.featureselection.ranking.Ranker;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;

/**
 * Interface of rankers that perform computations incrementally
 */
public interface IncrementalRanker extends Ranker {
  /**
   * Process new feature set - update statistics
   * 
   * @param set
   *          FeatureSet to process
   * @throws IllegalStateException
   */
  void process(FeatureSet set) throws IllegalStateException;

  /**
   * Compute ranking
   */
  void update();

  /**
   * @return True if stored ranking values are computed according to current statistics
   */
  boolean isUpdated();
}
