package org.apache.samoa.preprocessing.featureselection.topologies.common.evaluating;

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

import mockit.Mocked;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Selection;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class EvaluationRepositoryTest {
  @Mocked
  private Selection selection;

  @Test(expected = IllegalAccessException.class)
  public void testIllegalAccess() throws IllegalAccessException {
    EvaluationRepository repository = new EvaluationRepository();

    repository.updateEvaluationResult(1, 0d, 1d);
  }

  @Test
  public void testThatCreateSingleEvaluationGroupWorks() throws IllegalAccessException {
    EvaluationRepository repository = new EvaluationRepository();
    List<Selection> selections = Arrays.asList(selection, selection);

    int groupId = repository.createEvaluationGroup(selections);
    List<Evaluation> group = repository.getEvaluationGroup(groupId);

    assertEquals(2, group.size());
    assertTrue(group.get(0).getEvaluationId() != group.get(1).getEvaluationId());
  }

  @Test
  public void testThatCreateMultipleEvaluationGroupsWorks() throws IllegalAccessException {
    EvaluationRepository repository = new EvaluationRepository();
    List<Selection> selections1 = Arrays.asList(selection, selection);
    List<Selection> selections2 = Arrays.asList(selection, selection);

    int groupId1 = repository.createEvaluationGroup(selections1);
    int groupId2 = repository.createEvaluationGroup(selections2);
    List<Evaluation> group1 = repository.getEvaluationGroup(groupId1);
    List<Evaluation> group2 = repository.getEvaluationGroup(groupId2);

    assertEquals(2, group1.size());
    assertTrue(group1.get(0).getEvaluationId() != group1.get(1).getEvaluationId());
    assertEquals(2, group2.size());
    assertTrue(group2.get(0).getEvaluationId() != group2.get(1).getEvaluationId());

    assertTrue(groupId1 != groupId2);
    assertTrue(group1.get(0).getEvaluationId() != group2.get(0).getEvaluationId());
    assertTrue(group1.get(0).getEvaluationId() != group2.get(1).getEvaluationId());
    assertTrue(group1.get(1).getEvaluationId() != group2.get(0).getEvaluationId());
    assertTrue(group1.get(1).getEvaluationId() != group2.get(1).getEvaluationId());
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatRemoveEvaluationGroupWorks() throws IllegalAccessException {
    EvaluationRepository repository = new EvaluationRepository();
    List<Selection> selections1 = Arrays.asList(selection, selection);
    List<Selection> selections2 = Arrays.asList(selection, selection);

    int groupId1 = repository.createEvaluationGroup(selections1);
    int groupId2 = repository.createEvaluationGroup(selections2);
    List<Evaluation> group1 = repository.getEvaluationGroup(groupId1);
    repository.removeEvaluationGroup(groupId2);

    assertEquals(2, group1.size());
    assertTrue(group1.get(0).getEvaluationId() != group1.get(1).getEvaluationId());

    repository.getEvaluationGroup(groupId2);
  }

  @Test(expected = IllegalAccessException.class)
  public void testThatPurgeWorks() throws IllegalAccessException {
    EvaluationRepository repository = new EvaluationRepository();
    List<Selection> selections1 = Arrays.asList(selection, selection);

    int groupId1 = repository.createEvaluationGroup(selections1);
    repository.purge();
    repository.getEvaluationGroup(groupId1);
  }

  @Test
  public void testThatPrepareEvaluationGroupWorks() throws IllegalAccessException {
    EvaluationRepository repository = new EvaluationRepository();
    List<Selection> selections1 = Arrays.asList(selection, selection);
    List<Selection> selections2 = Arrays.asList(selection, selection);

    int groupId1 = repository.createEvaluationGroup(selections1);
    int groupId2 = repository.createEvaluationGroup(selections2);
    // below there's an assumption that ordered processing of selections and that evaluationId in repository starts from 0:
    boolean preparedGroup1First = repository.updateEvaluationResult(0, 0.5d, 1d);
    boolean preparedGroup1Second = repository.updateEvaluationResult(1, 0.5d, 1d);
    boolean preparedGroup2First = repository.updateEvaluationResult(2, 0.5d, 1d);
    List<Evaluation> group1 = repository.getEvaluationGroup(groupId1);
    List<Evaluation> group2 = repository.getEvaluationGroup(groupId2);

    assertFalse(preparedGroup1First);
    assertTrue(preparedGroup1Second);
    assertFalse(preparedGroup2First);

    assertEquals(2, group1.size());
    assertTrue(group1.get(0).getEvaluationId() != group1.get(1).getEvaluationId());
    assertTrue(group1.get(0).isPrepared());
    assertTrue(group1.get(1).isPrepared());
    assertEquals(2, group2.size());
    assertTrue(group2.get(0).getEvaluationId() != group2.get(1).getEvaluationId());
    assertTrue(group2.get(0).isPrepared());
    assertFalse(group2.get(1).isPrepared());

    assertTrue(groupId1 != groupId2);
    assertTrue(group1.get(0).getEvaluationId() != group2.get(0).getEvaluationId());
    assertTrue(group1.get(0).getEvaluationId() != group2.get(1).getEvaluationId());
    assertTrue(group1.get(1).getEvaluationId() != group2.get(0).getEvaluationId());
    assertTrue(group1.get(1).getEvaluationId() != group2.get(1).getEvaluationId());
  }
}
