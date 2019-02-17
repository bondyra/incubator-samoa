package org.apache.samoa.preprocessing.featureselection.ranking;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FeatureFilterTest {
  @Tested
  private FeatureFilter featureFilter;

  @Before
  public void setUp() {
    featureFilter = new FeatureFilter();
  }

  @Test
  public void testThatProcessInstanceWorks(@Mocked final Instance instance, @Mocked final Selection selection) {
    new Expectations() {
      {
        selection.getSelectedIndexes();
        returns(Arrays.asList(0, 1, 2));
        selection.getDimensionality();
        result = 3;
        instance.classValue();
        returns(3d);
        instance.weight();
        returns(1d);
        instance.value(0);
        result = 0.5d;
        instance.value(1);
        result = 1d;
        instance.value(2);
        result = 2d;
      }
    };

    featureFilter.setSelection(selection);
    Instance filteredInstance = featureFilter.processInstance(instance);

    assertEquals(4, filteredInstance.numAttributes());
    assertEquals(0.5d, filteredInstance.value(0), Double.MIN_VALUE);
    assertEquals(1d, filteredInstance.value(1), Double.MIN_VALUE);
    assertEquals(2d, filteredInstance.value(2), Double.MIN_VALUE);
    assertEquals(3d, filteredInstance.classValue(), Double.MIN_VALUE);
    assertEquals(1d, filteredInstance.weight(), Double.MIN_VALUE);

    new FullVerifications() {
      {
        selection.getSelectedIndexes();
        times = 2; // 1 for initialize and 1 for process
        selection.getDimensionality();
        times = 1;
        List<Integer> capturedIndices = new ArrayList<>();
        instance.attribute(withCapture(capturedIndices));
        assertTrue(CollectionUtils.isEqualCollection(new HashSet<>(capturedIndices), Arrays.asList(0, 1, 2)));
        instance.classAttribute();
        times = 1;

      }
    };
  }

  @Test
  public void testThatProcessInstanceNotUniqueIndicesWorks(@Mocked final Instance instance,
      @Mocked final Selection selection) {
    new Expectations() {
      {
        selection.getSelectedIndexes();
        returns(Arrays.asList(0, 1, 2));
        selection.getDimensionality();
        result = 3;
        instance.classValue();
        returns(3d);
        instance.weight();
        returns(1d);
        instance.value(0);
        result = 0.5d;
        instance.value(1);
        result = 1d;
        instance.value(2);
        result = 2d;
      }
    };

    featureFilter.setSelection(selection);
    Instance filteredInstance = featureFilter.processInstance(instance);

    assertEquals(4, filteredInstance.numAttributes());
    assertEquals(0.5d, filteredInstance.value(0), Double.MIN_VALUE);
    assertEquals(1d, filteredInstance.value(1), Double.MIN_VALUE);
    assertEquals(2d, filteredInstance.value(2), Double.MIN_VALUE);
    assertEquals(3d, filteredInstance.classValue(), Double.MIN_VALUE);
    assertEquals(1d, filteredInstance.weight(), Double.MIN_VALUE);

    new FullVerifications() {
      {
        selection.getSelectedIndexes();
        times = 2; // 1 for initialize and 1 for process
        selection.getDimensionality();
        times = 1;
        List<Integer> capturedIndices = new ArrayList<>();
        instance.attribute(withCapture(capturedIndices));
        assertTrue(CollectionUtils.isEqualCollection(new HashSet<>(capturedIndices), Arrays.asList(0, 1, 2)));
        instance.classAttribute();
        times = 1;
      }
    };
  }

  @Test
  public void testThatProcessInstanceWithResettingWorks(@Mocked final Instance instance,
      @Mocked final Selection selection) {
    new Expectations() {
      {
        selection.getSelectedIndexes();
        returns(Arrays.asList(0, 1, 2), Arrays.asList(0, 1, 2), Arrays.asList(0, 1, 2),
            Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3));
        selection.getDimensionality();
        result = 3;
        instance.value(0);
        returns(0.5d);
        instance.value(1);
        returns(1d, 2d);
        instance.value(2);
        returns(2d, 3d);
        instance.value(3);
        result = 4d;
        instance.classValue();
        returns(3d, 5d);
        instance.weight();
        returns(1d, 2d);
        selection.equalTo(selection);
        result = false;
      }
    };

    featureFilter.setSelection(selection);
    featureFilter.processInstance(instance);
    featureFilter.setSelection(selection);
    Instance filteredInstance = featureFilter.processInstance(instance);

    assertEquals(filteredInstance.numAttributes(), 4);
    assertEquals(2d, filteredInstance.value(0), Double.MIN_VALUE);
    assertEquals(3d, filteredInstance.value(1), Double.MIN_VALUE);
    assertEquals(4d, filteredInstance.value(2), Double.MIN_VALUE);
    assertEquals(5d, filteredInstance.classValue(), Double.MIN_VALUE);
    assertEquals(2d, filteredInstance.weight(), Double.MIN_VALUE);

    new FullVerifications() {
      {
        selection.getSelectedIndexes();
        times = 4; // 2 for initialize and 2 for process
        selection.getDimensionality();
        times = 2;
        List<Integer> capturedIndices = new ArrayList<>();
        instance.attribute(withCapture(capturedIndices));
        assertTrue(CollectionUtils.isEqualCollection(new HashSet<>(capturedIndices), Arrays.asList(0, 1, 2)));
        instance.classAttribute();
        times = 2;

      }
    };
  }
}
