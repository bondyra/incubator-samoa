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

import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureType;
import org.apache.samoa.preprocessing.featureselection.ranking.models.ValueIndex;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Builds feature set step by step. Builder is specific for particular instance and particular features' type
 */
public class FeatureSetBuilder {
  private final double labelValue;
  private final FeatureType type;
  private final long instanceNumber;

  /**
   * Collection of feature indexes and their values that is being built
   */
  private Collection<ValueIndex> valueIndexes = new ArrayList<>();

  /**
   * Initialize feature set builder basing on information of particular instance
   * 
   * @param labelValue
   *          Label value of particular instance
   * @param type
   *          Designated type of features
   * @param instanceNumber
   *          Number of particular instance
   */
  public FeatureSetBuilder(double labelValue, FeatureType type, long instanceNumber) {
    this.labelValue = labelValue;
    this.type = type;
    this.instanceNumber = instanceNumber;
  }

  /**
   * Add new feature to current builder state
   * 
   * @param index
   *          Index of new feature
   * @param value
   *          Value of new feature
   */
  public void appendFeature(int index, double value) {
    valueIndexes.add(new ValueIndex(index, value));
  }

  /**
   * Reset builder state
   */
  public void reset() {
    valueIndexes = new ArrayList<>();
  }

  /**
   * @return Collection of known feature indexes with values
   */
  public Collection<ValueIndex> getValueIndexes() {
    return valueIndexes;
  }

  /**
   * @return Label value of particular instance
   */
  public double getLabelValue() {
    return labelValue;
  }

  /**
   * @return Type of features
   */
  public FeatureType getType() {
    return type;
  }

  /**
   * @return Number of particular instance
   */
  public long getInstanceNumber() {
    return instanceNumber;
  }
}
