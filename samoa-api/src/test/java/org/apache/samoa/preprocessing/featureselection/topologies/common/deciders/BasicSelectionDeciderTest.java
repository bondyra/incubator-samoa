package org.apache.samoa.preprocessing.featureselection.topologies.common.deciders;

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
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import org.apache.commons.collections.CollectionUtils;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.InstanceRanking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.apache.samoa.preprocessing.featureselection.topologies.events.SelectionContentEvent;
import org.apache.samoa.topology.Stream;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class BasicSelectionDeciderTest {
  @Mocked
  private Stream stream;

  @Tested
  private BasicSelectionDecider decider;

  @Before
  public void setUp() {
    decider = new BasicSelectionDecider();
    decider.rankingSumDecisionBoundaryOption.setValue(5);
    decider.setDecisionStream(stream);
  }

  @Test
  public void testThatSingleProcessSelectionsForSimpleUseCaseWorks() throws IllegalAccessException {
    Selection selection = new Selection(Arrays.asList(0, 1, 2));
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.1, 1, 0.2)));
    ranking.updateNumericRanking(new Ranking(ImmutableMap.of(2, 0.5)));

    Collection<Selection> selections = Collections.singletonList(selection);
    decider.processSelections(selections, ranking);

    new Verifications() {
      {
        SelectionContentEvent event;

        stream.put(event = withCapture());
        times = 1;

        assertTrue(
            CollectionUtils.isEqualCollection(event.getSelection().getSelectedIndexes(), Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatSingleProcessSelectionsWhenSecondSelectionIsWorseWorks() throws IllegalAccessException {
    Selection selection1 = new Selection(Arrays.asList(0, 1, 2));
    Selection selection2 = new Selection(Arrays.asList(1, 2));
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.3, 1, 0.2)));
    ranking.updateNumericRanking(new Ranking(ImmutableMap.of(2, 0.5)));

    Collection<Selection> selections = Arrays.asList(selection1, selection2);
    decider.processSelections(selections, ranking);

    new Verifications() {
      {
        SelectionContentEvent event;

        stream.put(event = withCapture());
        times = 1;
        assertTrue(
            CollectionUtils.isEqualCollection(event.getSelection().getSelectedIndexes(), Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatSingleProcessSelectionsWhenSecondSelectionIsBetterWorks() throws IllegalAccessException {
    Selection selection1 = new Selection(Arrays.asList(1, 2));
    Selection selection2 = new Selection(Arrays.asList(0, 1, 2));
    InstanceRanking ranking = new InstanceRanking();
    ranking.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.1, 1, 0.2)));
    ranking.updateNumericRanking(new Ranking(ImmutableMap.of(2, 0.5)));

    Collection<Selection> selections = Arrays.asList(selection1, selection2);
    decider.processSelections(selections, ranking);

    new Verifications() {
      {
        SelectionContentEvent event;

        stream.put(event = withCapture());
        times = 1;
        System.out.println(event.getSelection().getSelectedIndexes());
        assertTrue(
            CollectionUtils.isEqualCollection(event.getSelection().getSelectedIndexes(), Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatMultipleProcessSelectionsWhenSecondProcessHasWorseSelectionWorks() throws IllegalAccessException {
    Selection selection1 = new Selection(Arrays.asList(0, 1, 2));
    Selection selection2 = new Selection(Arrays.asList(1, 2));
    InstanceRanking ranking1 = new InstanceRanking();
    ranking1.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.1, 1, 0.2)));
    ranking1.updateNumericRanking(new Ranking(ImmutableMap.of(2, 0.5)));
    InstanceRanking ranking2 = new InstanceRanking();
    ranking2.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.1, 1, 0.2)));
    ranking2.updateNumericRanking(new Ranking(ImmutableMap.of(2, 0.5)));

    Collection<Selection> selections1 = Collections.singletonList(selection1);
    Collection<Selection> selections2 = Collections.singletonList(selection2);
    decider.processSelections(selections1, ranking1);
    decider.processSelections(selections2, ranking2);

    new Verifications() {
      {
        SelectionContentEvent event;

        stream.put(event = withCapture());
        times = 1;

        assertTrue(
            CollectionUtils.isEqualCollection(event.getSelection().getSelectedIndexes(), Arrays.asList(0, 1, 2)));
      }
    };
  }

  @Test
  public void testThatMultipleProcessSelectionsWhenSecondProcessHasBetterSelectionWorks()
      throws IllegalAccessException {
    Selection selection1 = new Selection(Arrays.asList(0, 1));
    Selection selection2 = new Selection(Arrays.asList(0, 1, 2));
    InstanceRanking ranking1 = new InstanceRanking();
    ranking1.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.1, 1, 0.2)));
    ranking1.updateNumericRanking(new Ranking(ImmutableMap.of(2, 0.5)));
    InstanceRanking ranking2 = new InstanceRanking();
    ranking2.updateNominalRanking(new Ranking(ImmutableMap.of(0, 0.1, 1, 0.2)));
    ranking2.updateNumericRanking(new Ranking(ImmutableMap.of(2, 1.1)));

    Collection<Selection> selections1 = Collections.singletonList(selection1);
    Collection<Selection> selections2 = Collections.singletonList(selection2);
    decider.processSelections(selections1, ranking1);
    decider.processSelections(selections2, ranking2);

    new Verifications() {
      {
        List<SelectionContentEvent> events = new ArrayList<>();

        stream.put(withCapture(events));
        times = 2;

        assertTrue(
            CollectionUtils.isEqualCollection(events.get(0).getSelection().getSelectedIndexes(), Arrays.asList(0, 1)));
        assertTrue(CollectionUtils.isEqualCollection(events.get(1).getSelection().getSelectedIndexes(),
            Arrays.asList(0, 1, 2)));
      }
    };
  }
}
