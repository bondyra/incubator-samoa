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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FeatureSetBuilderTest {
  @Test
  public void testThatFeatureSetBuilderForNormalUseCaseWorks() {
    FeatureSetBuilder builder = new FeatureSetBuilder(1d, FeatureType.Nominal, 1);

    builder.appendFeature(0, 1d);
    builder.appendFeature(2, 3d);

    assertEquals(FeatureType.Nominal, builder.getType());
    assertEquals(1d, builder.getLabelValue(), Double.MIN_VALUE);
    assertEquals(1, builder.getInstanceNumber());
    assertEquals(2, builder.getValueIndexes().size());
    for (ValueIndex vi : builder.getValueIndexes()) {
      if (vi.getIndex() == 0) {
        assertEquals(new Integer(0), vi.getIndex());
        assertEquals(1d, vi.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(2), vi.getIndex());
        assertEquals(3d, vi.getValue(), Double.MIN_VALUE);
      }
    }
  }

  @Test
  public void testThatFeatureSetBuilderWithResettingWorks() {
    FeatureSetBuilder builder = new FeatureSetBuilder(1d, FeatureType.Nominal, 1);

    builder.appendFeature(0, 1d);
    builder.appendFeature(2, 3d);
    builder.reset();

    assertEquals(FeatureType.Nominal, builder.getType());
    assertEquals(1d, builder.getLabelValue(), Double.MIN_VALUE);
    assertEquals(1, builder.getInstanceNumber());
    assertEquals(0, builder.getValueIndexes().size());
  }
}
