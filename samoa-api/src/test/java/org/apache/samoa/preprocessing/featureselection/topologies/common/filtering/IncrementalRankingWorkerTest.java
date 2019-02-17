package org.apache.samoa.preprocessing.featureselection.topologies.common.filtering;

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
import mockit.*;
import org.apache.samoa.preprocessing.featureselection.ranking.incremental.IncrementalRanker;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.ranking.models.Ranking;
import org.apache.samoa.preprocessing.featureselection.topologies.events.RankingContentEvent;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IncrementalRankingWorkerTest {
    @Injectable
    private IncrementalRanker numericRanker;
    @Injectable
    private IncrementalRanker alternativeNumericRanker;
    @Injectable
    private IncrementalRanker nominalRanker;
    @Injectable
    private IncrementalRanker alternativeNominalRanker;

    @Mocked
    private FeatureSet numericFeatureSet;
    @Mocked
    private FeatureSet nominalFeatureSet;

    @Test
    public void testThatResetWorks(){
        IncrementalRankingWorker worker = new IncrementalRankingWorker(
                0, numericRanker, nominalRanker, alternativeNumericRanker, alternativeNominalRanker,
                100, 100
        );

        worker.reset();

        new Verifications(){
            {
                alternativeNominalRanker.reset();
                times = 1;
                alternativeNumericRanker.reset();
                times = 1;
                nominalRanker.reset();
                times = 1;
                numericRanker.reset();
                times = 1;
            }
        };
    }

    @Test
    public void testThatProcessForSimpleUseCaseWorks(){
        IncrementalRankingWorker worker = new IncrementalRankingWorker(
            0, numericRanker, nominalRanker, alternativeNumericRanker, alternativeNominalRanker,
                100, 100
        );

        boolean result = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);

        assertFalse(result);

        new Verifications(){
            {
                alternativeNominalRanker.process(withInstanceOf(FeatureSet.class));
                times = 0;
                alternativeNumericRanker.process(withInstanceOf(FeatureSet.class));
                times = 0;

                nominalRanker.process(nominalFeatureSet);
                times = 1;
                numericRanker.process(numericFeatureSet);
                times = 1;
            }
        };
    }

    @Test
    public void testThatWarningSignalIsHandled(){
        IncrementalRankingWorker worker = new IncrementalRankingWorker(
                0, numericRanker, nominalRanker, alternativeNumericRanker, alternativeNominalRanker,
                100, 100
        );

        boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        worker.processWarning();
        boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);

        assertFalse(result1);
        assertFalse(result2);

        new Verifications(){
            {
                alternativeNominalRanker.process(nominalFeatureSet);
                times = 1;
                alternativeNumericRanker.process(numericFeatureSet);
                times = 1;

                nominalRanker.process(nominalFeatureSet);
                times = 2;
                numericRanker.process(numericFeatureSet);
                times = 2;
            }
        };
    }

    @Test
    public void testThatProcessForWarningSignalAndFinishedAlternativePeriodWorks(){
        IncrementalRankingWorker worker = new IncrementalRankingWorker(
                0, numericRanker, nominalRanker, alternativeNumericRanker, alternativeNominalRanker,
                1, 100
        );

        boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        worker.processWarning();
        boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);

        assertFalse(result1);
        assertFalse(result2);
        assertFalse(result3);

        new Verifications(){
            {
                // alternative ranking period is set to 1, so only one invocation is expected:
                alternativeNominalRanker.process(nominalFeatureSet);
                times = 1;
                alternativeNumericRanker.process(numericFeatureSet);
                times = 1;
                alternativeNominalRanker.reset();
                times = 1;
                alternativeNumericRanker.reset();
                times = 1;

                nominalRanker.process(nominalFeatureSet);
                times = 3;
                numericRanker.process(numericFeatureSet);
                times = 3;
            }
        };
    }

    @Test
    public void testThatGetEventForNormalUseCaseWorks(){
        IncrementalRankingWorker worker = new IncrementalRankingWorker(
                0, numericRanker, nominalRanker, alternativeNumericRanker, alternativeNominalRanker,
                1, 2
        );
        new Expectations(){
            {
                nominalRanker.getRanking();
                result = new Ranking(ImmutableMap.of(0, 1d,1, 0.5d));
                numericRanker.getRanking();
                result = new Ranking(ImmutableMap.of(2, 1d,3, 0.5d));
            }
        };
        boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        RankingContentEvent event = worker.getEvent();

        assertFalse(result1);
        assertTrue(result2);
        assertEquals(0, event.getRankingIdentifier());
        for (Map.Entry<Integer, Double> entry : event.getNominalRanking().getRankingValues().entrySet()){
            if (entry.getKey() == 0){
                assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
            }
            else{
                assertEquals(new Integer(1), entry.getKey());
                assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
            }
        }
        for (Map.Entry<Integer, Double> entry : event.getNumericRanking().getRankingValues().entrySet()){
            if (entry.getKey() == 2){
                assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
            }
            else{
                assertEquals(new Integer(3), entry.getKey());
                assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
            }
        }

        new Verifications(){
            {
                nominalRanker.process(nominalFeatureSet);
                times = 2;
                numericRanker.process(numericFeatureSet);
                times = 2;

                nominalRanker.getRanking();
                times = 1;
                numericRanker.getRanking();
                times = 1;
            }
        };
    }

    @Test
    public void testThatProcessForMoreComplexUseCaseWorks(){
        IncrementalRankingWorker worker = new IncrementalRankingWorker(
                0, numericRanker, nominalRanker, alternativeNumericRanker, alternativeNominalRanker,
                2, 3
        );
        new Expectations(){
            {
                alternativeNominalRanker.getRanking();
                result = new Ranking(ImmutableMap.of(0, 0.5d, 1, 1d));
                alternativeNumericRanker.getRanking();
                result = new Ranking(ImmutableMap.of(2, 0.5d, 3, 1d));
            }
        };
        boolean result1 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        worker.processWarning();
        boolean result2 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        boolean result3 = worker.processFeatureSets(numericFeatureSet, nominalFeatureSet);
        worker.processDrift();
        RankingContentEvent event = worker.getEvent();

        assertFalse(result1);
        assertFalse(result2);
        assertTrue(result3);
        assertEquals(0, event.getRankingIdentifier());
        // values for alternative ranking:
        for (Map.Entry<Integer, Double> entry : event.getNominalRanking().getRankingValues().entrySet()){
            if (entry.getKey() == 0){
                assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
            }
            else{
                assertEquals(new Integer(1), entry.getKey());
                assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
            }
        }
        for (Map.Entry<Integer, Double> entry : event.getNumericRanking().getRankingValues().entrySet()){
            if (entry.getKey() == 2){
                assertEquals(0.5d, entry.getValue(), Double.MIN_VALUE);
            }
            else{
                assertEquals(new Integer(3), entry.getKey());
                assertEquals(1d, entry.getValue(), Double.MIN_VALUE);
            }
        }

        new Verifications(){
            {
                nominalRanker.process(nominalFeatureSet);
                times = 3;
                numericRanker.process(numericFeatureSet);
                times = 3;
                alternativeNominalRanker.process(nominalFeatureSet);
                times = 2;
                alternativeNumericRanker.process(numericFeatureSet);
                times = 2;

                alternativeNominalRanker.getRanking();
                times = 1;
                alternativeNumericRanker.getRanking();
                times = 1;
            }
        };
    }
}
