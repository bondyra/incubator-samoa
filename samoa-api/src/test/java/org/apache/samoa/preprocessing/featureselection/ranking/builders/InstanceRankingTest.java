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

import com.google.common.collect.ImmutableMap;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class InstanceRankingTest {
  @Test
  public void testThatInstanceRankingForNormalUseCaseWorks() throws IllegalAccessException {
    InstanceRanking ranking = new InstanceRanking();

    ranking.updateNominalRanking(
        new Ranking(
            ImmutableMap.of(
                1, 1d,
                2, 3d)));
    ranking.updateNumericRanking(
        new Ranking(
            ImmutableMap.of(
                3, 1d,
                4, 3d)));

    assertEquals(2, ranking.getNominalRankingValues().size());
    for (Map.Entry<Integer, Double> entry : ranking.getNominalRankingValues().entrySet()) {
      if (entry.getKey() == 1) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(2), entry.getKey());
        assertEquals(3d, entry.getValue(), Double.MIN_VALUE);
      }
    }
    assertEquals(2, ranking.getNumericRankingValues().size());
    for (Map.Entry<Integer, Double> entry : ranking.getNumericRankingValues().entrySet()) {
      if (entry.getKey() == 3) {
        assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(4), entry.getKey());
        assertEquals(3d, entry.getValue(), Double.MIN_VALUE);
      }
    }
  }
}
