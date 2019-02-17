package org.apache.samoa.preprocessing.featureselection.ranking.models;

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

import java.io.Serializable;
import java.util.Map;

/**
 * Stores relevancies of specific features
 */
public class Ranking implements Serializable {
  private Map<Integer, Double> rankingValues;

  /**
   * Create Ranking instance
   * 
   * @param rankingValues
   *          Map of feature index and computed feature relevancy
   */
  public Ranking(Map<Integer, Double> rankingValues) {
    this.rankingValues = rankingValues;
  }

  /**
   * @return Map of feature index and computed feature relevancy
   * @throws IllegalStateException
   *           If ranking is undefined
   */
  public Map<Integer, Double> getRankingValues() throws IllegalStateException {
    if (rankingValues == null)
      throw new IllegalStateException();
    return rankingValues;
  }
}
