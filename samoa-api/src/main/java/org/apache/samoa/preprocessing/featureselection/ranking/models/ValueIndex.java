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

/**
 * Links feature index with its value
 */
public class ValueIndex {
  private Integer index;
  private Double value;

  /**
   * Create ValueIndex instance
   * 
   * @param index
   *          Index of the feature
   * @param value
   *          Value of the feature
   */
  public ValueIndex(int index, double value) {
    this.index = index;
    this.value = value;
  }

  public Integer getIndex() {
    return index;
  }

  public Double getValue() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ValueIndex) {
      ValueIndex vi = (ValueIndex) obj;
      return index.equals(vi.getIndex()) && value.equals(vi.getValue());
    }
    return false;
  }

  @Override
  public String toString() {
    return "Value: " + value + ", Index: " + index;
  }
}
