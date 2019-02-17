package org.apache.samoa.preprocessing.featureselection.topologies.common.splitters;

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

import mockit.*;
import org.apache.samoa.instances.Attribute;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureType;
import org.apache.samoa.preprocessing.featureselection.ranking.models.ValueIndex;
import org.apache.samoa.preprocessing.featureselection.topologies.events.FeatureSetContentEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FixedInstanceSplitterTest {
  @Tested
  private FixedInstanceSplitter splitter;

  @Before
  public void setUp() {
    splitter = new FixedInstanceSplitter();
  }

  @Test
  public void testThatSplitForSimpleUseCaseWorks(@Mocked final Instance instance, @Mocked final Attribute attribute) {
    new Expectations() {
      {
        instance.attribute(anyInt);
        result = attribute;

        instance.numAttributes();
        result = 2;

        instance.index(anyInt);
        returns(1, 2);

        instance.classIndex();
        result = 2;

        instance.classValue();
        result = 1d;

        instance.value(1);
        result = 2d;

        attribute.isNominal();
        result = false;
        attribute.isNumeric();
        result = true;
      }
    };

    splitter.setRankingIdentifiers(Collections.singletonList(1));
    splitter.maxFeaturesInSplitOption.setValue(1);
    List<FeatureSetContentEvent> events = splitter.split(instance);

    assertEquals(1, events.size());
    assertEquals(1, events.get(0).getRankingIdentifier());
    assertEquals("1-1", events.get(0).getKey());

    assertEquals(splitter.getInstanceNumber(), events.get(0).getNominalSet().getInstanceNumber());
    assertEquals(1d, events.get(0).getNominalSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Nominal, events.get(0).getNominalSet().getType());
    assertEquals(0, events.get(0).getNominalSet().getValueIndexes().size());

    assertEquals(splitter.getInstanceNumber(), events.get(0).getNumericSet().getInstanceNumber());
    assertEquals(1d, events.get(0).getNumericSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Numeric, events.get(0).getNumericSet().getType());
    assertEquals(1, events.get(0).getNumericSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(0).getNumericSet().getValueIndexes()) {
      assertEquals(new Integer(1), vi.getIndex());
      assertEquals(2d, vi.getValue(), Double.MIN_VALUE);
    }
  }

  @Test
  public void testThatSplitForMoreComplexUseCaseWorks(@Mocked final Instance instance,
      @Mocked final Attribute attribute) {
    new Expectations() {
      {
        instance.attribute(anyInt);
        result = attribute;

        instance.numAttributes();
        result = 5;

        instance.index(anyInt);
        returns(1, 2, 3, 4, 5);

        instance.classIndex();
        result = 4;

        instance.classValue();
        result = 4d;

        instance.value(1);
        result = Double.NaN;
        instance.value(2);
        result = 2d;
        instance.value(3);
        result = 3d;
        instance.value(5);
        result = 5d;

        attribute.isNominal();
        returns(false, true, true, true);
        attribute.isNumeric();
        returns(true, false, false, false);
      }
    };

    splitter.setRankingIdentifiers(Arrays.asList(1, 2));
    splitter.maxFeaturesInSplitOption.setValue(2);
    List<FeatureSetContentEvent> events = splitter.split(instance);

    assertEquals(4, events.size());

    // first event - for ranking 1, numeric+nominal value
    assertEquals(1, events.get(0).getRankingIdentifier());
    assertEquals("1-1", events.get(0).getKey());

    assertEquals(splitter.getInstanceNumber(), events.get(0).getNominalSet().getInstanceNumber());
    assertEquals(4d, events.get(0).getNominalSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Nominal, events.get(0).getNominalSet().getType());
    assertEquals(1, events.get(0).getNominalSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(0).getNominalSet().getValueIndexes()) {
      assertEquals(new Integer(2), vi.getIndex());
      assertEquals(2d, vi.getValue(), Double.MIN_VALUE);
    }

    assertEquals(splitter.getInstanceNumber(), events.get(0).getNumericSet().getInstanceNumber());
    assertEquals(4d, events.get(0).getNumericSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Numeric, events.get(0).getNumericSet().getType());
    assertEquals(1, events.get(0).getNumericSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(0).getNumericSet().getValueIndexes()) {
      assertEquals(new Integer(1), vi.getIndex());
      assertTrue(Double.isNaN(vi.getValue()));
    }

    // second event - for ranking 2, numeric + nominal value
    assertEquals(2, events.get(1).getRankingIdentifier());
    assertEquals("1-2", events.get(1).getKey());

    assertEquals(splitter.getInstanceNumber(), events.get(1).getNominalSet().getInstanceNumber());
    assertEquals(4d, events.get(1).getNominalSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Nominal, events.get(1).getNominalSet().getType());
    assertEquals(1, events.get(1).getNominalSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(1).getNominalSet().getValueIndexes()) {
      assertEquals(new Integer(2), vi.getIndex());
      assertEquals(2d, vi.getValue(), Double.MIN_VALUE);
    }

    assertEquals(splitter.getInstanceNumber(), events.get(1).getNumericSet().getInstanceNumber());
    assertEquals(4d, events.get(1).getNumericSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Numeric, events.get(1).getNumericSet().getType());
    assertEquals(1, events.get(1).getNumericSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(1).getNumericSet().getValueIndexes()) {
      assertEquals(new Integer(1), vi.getIndex());
      assertTrue(Double.isNaN(vi.getValue()));
    }

    // third event - for ranking 1, two nominal values
    assertEquals(1, events.get(2).getRankingIdentifier());
    assertEquals("2-1", events.get(2).getKey());

    assertEquals(splitter.getInstanceNumber(), events.get(2).getNominalSet().getInstanceNumber());
    assertEquals(4d, events.get(2).getNominalSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Nominal, events.get(2).getNominalSet().getType());
    assertEquals(2, events.get(2).getNominalSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(2).getNominalSet().getValueIndexes()) {
      if (vi.getIndex() == 3) {
        assertEquals(3d, vi.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(5), vi.getIndex());
        assertEquals(5d, vi.getValue(), Double.MIN_VALUE);

      }
    }
    assertEquals(splitter.getInstanceNumber(), events.get(2).getNumericSet().getInstanceNumber());
    assertEquals(4d, events.get(2).getNumericSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Numeric, events.get(2).getNumericSet().getType());
    assertEquals(0, events.get(2).getNumericSet().getValueIndexes().size());

    // fourth event - for ranking 2, two nominal values
    assertEquals(2, events.get(3).getRankingIdentifier());
    assertEquals("2-2", events.get(3).getKey());

    assertEquals(splitter.getInstanceNumber(), events.get(3).getNominalSet().getInstanceNumber());
    assertEquals(4d, events.get(3).getNominalSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Nominal, events.get(3).getNominalSet().getType());
    assertEquals(2, events.get(3).getNominalSet().getValueIndexes().size());
    for (ValueIndex vi : events.get(3).getNominalSet().getValueIndexes()) {
      if (vi.getIndex() == 3) {
        assertEquals(3d, vi.getValue(), Double.MIN_VALUE);
      } else {
        assertEquals(new Integer(5), vi.getIndex());
        assertEquals(5d, vi.getValue(), Double.MIN_VALUE);

      }
    }
    assertEquals(splitter.getInstanceNumber(), events.get(3).getNumericSet().getInstanceNumber());
    assertEquals(4d, events.get(3).getNumericSet().getLabelValue(), Double.MIN_VALUE);
    assertEquals(FeatureType.Numeric, events.get(3).getNumericSet().getType());
    assertEquals(0, events.get(3).getNumericSet().getValueIndexes().size());
  }
}
