package org.apache.samoa.preprocessing.featureselection.ranking.selectors;

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

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Common methods used in feature selectors
 */
public class Utils {

  /**
   * Get key from key value map entry
   */
  public static class GetKeyFromEntryFunction implements Function<Map.Entry<Integer, Double>, Integer> {
    @Nullable
    @Override
    public Integer apply(@Nullable Map.Entry<Integer, Double> input) {
      return input == null ? null : input.getKey();
    }
  }

  /**
   * Predicate for threshold feature selection
   */
  public static class RankigAbsThresholdPredicate implements Predicate<Map.Entry<Integer, Double>> {
    private double threshold;

    public RankigAbsThresholdPredicate(double threshold) {
      this.threshold = threshold;
    }

    @Override
    public boolean apply(@Nullable Map.Entry<Integer, Double> input) {
      return input != null && Math.abs(input.getValue()) >= threshold;
    }
  }
}
