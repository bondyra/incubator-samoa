package org.apache.samoa.preprocessing.featureselection.ranking.builders;

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

import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;

import java.util.Map;

/**
 * Common ranking that aggregates rankings for nominal and numeric rankings
 */
public class InstanceRanking {
  /**
   * Ranking for numeric attributes
   */
  private Ranking numericRanking;
  /**
   * Ranking for nominal attributes
   */
  private Ranking nominalRanking;

  /**
   * Create InstanceRanking instance
   */
  public InstanceRanking() {
    numericRanking = null;
    nominalRanking = null;
  }

  /**
   * Set new ranking for nominal attributes
   * 
   * @param newNominalRanking
   *          New ranking for nominal attributes
   */
  public void updateNominalRanking(Ranking newNominalRanking) {
    nominalRanking = newNominalRanking;
  }

  /**
   * Set new ranking for numeric attributes
   * 
   * @param newNumericRanking
   *          New ranking for numeric attributes
   */
  public void updateNumericRanking(Ranking newNumericRanking) {
    numericRanking = newNumericRanking;
  }

  /**
   * @return Ranking indexes and values for numeric attributes
   * @throws IllegalAccessException
   *           If ranking for numeric attributes is undefined
   */
  public Map<Integer, Double> getNumericRankingValues() throws IllegalAccessException {
    if (numericRanking == null)
      throw new IllegalAccessException("Numeric ranking not found.");
    return numericRanking.getRankingValues();
  }

  /**
   * @return Ranking indexes and values for nominal attributes
   * @throws IllegalAccessException
   *           If ranking for nominal attributes is undefined
   */
  public Map<Integer, Double> getNominalRankingValues() throws IllegalAccessException {
    if (nominalRanking == null)
      throw new IllegalAccessException("Nominal ranking not found.");
    return nominalRanking.getRankingValues();
  }
}
