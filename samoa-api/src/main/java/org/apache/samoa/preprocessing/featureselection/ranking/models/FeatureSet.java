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

import org.apache.samoa.preprocessing.featureselection.ranking.builders.FeatureSetBuilder;

import java.io.Serializable;
import java.util.Collection;

/**
 * Partial instance information
 */
public class FeatureSet implements Serializable {
  private final Collection<ValueIndex> valueIndexes;
  private final double labelValue;
  private final FeatureType type;
  private final long instanceNumber;

  /**
   * Create feature set basing on directly provided values
   * 
   * @param valueIndexes
   *          Collection of feature indexes and its values
   * @param labelValue
   *          Label value of particular instance
   * @param type
   *          Designated features' type
   * @param instanceNumber
   *          Number of particular instance
   */
  public FeatureSet(Collection<ValueIndex> valueIndexes, double labelValue, FeatureType type, long instanceNumber) {
    this.valueIndexes = valueIndexes;
    this.labelValue = labelValue;
    this.type = type;
    this.instanceNumber = instanceNumber;
  }

  /**
   * Create feature set basing on feature set builder
   * 
   * @param builder
   *          Feature set builder
   */
  public FeatureSet(FeatureSetBuilder builder) {
    this.valueIndexes = builder.getValueIndexes();
    this.labelValue = builder.getLabelValue();
    this.type = builder.getType();
    this.instanceNumber = builder.getInstanceNumber();
  }

  /**
   * @return Collection of feature indexes and its values
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
   * @return Designated features' type
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

  @Override
  public String toString() {
    return "FeatureSet, values: "
        + valueIndexes.toString()
        + ", label value: "
        + labelValue
        + ", features type: "
        + type
        + ", instance number: "
        + instanceNumber;
  }
}