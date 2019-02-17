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

/**
 * IncrementalRanker creator
 */
public class IncrementalRankerCreator {
  /**
   * Create IncrementalRanker instance
   * 
   * @param ranker
   *          Name of class to be instantiated
   * @return Instantiated class
   */
  public IncrementalRanker getRanker(String ranker) {
    switch (ranker) {
    case "InformationGainIncrementalRanker":
      return new InformationGainIncrementalRanker();
    case "SymmetricalUncertaintyIncrementalRanker":
      return new SymmetricalUncertaintyIncrementalRanker();
    case "PearsonCorrelationIncrementalRanker":
      return new PearsonCorrelationIncrementalRanker();
    }
    return null;
  }
}
