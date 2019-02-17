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

import com.github.javacliparser.IntOption;
import org.apache.samoa.instances.Instance;
import org.apache.samoa.preprocessing.featureselection.ranking.builders.FeatureSetBuilder;
import org.apache.samoa.preprocessing.featureselection.topologies.events.FeatureSetContentEvent;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureSet;
import org.apache.samoa.preprocessing.featureselection.ranking.models.FeatureType;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class FixedInstanceSplitter implements Splitter {
  public IntOption maxFeaturesInSplitOption = new IntOption("maxFeaturesInSplit",
      'm', "How many features are to be contained in one set of partial information?",
      Integer.MAX_VALUE, 1, Integer.MAX_VALUE); // max value - to get no splits at all

  private Collection<Integer> rankingIdentifiers;
  private long instanceNumber = 0;

  public FixedInstanceSplitter() {

  }

  @Override
  public List<FeatureSetContentEvent> split(Instance instance) {
    List<FeatureSetContentEvent> events = new LinkedList<>();
    int featuresInSplit = 0;
    int chunkNumber = 1;
    instanceNumber++;
    FeatureSetBuilder nominalFeatureSetBuilder = new FeatureSetBuilder(instance.classValue(), FeatureType.Nominal,
        instanceNumber);
    FeatureSetBuilder numericFeatureSetBuilder = new FeatureSetBuilder(instance.classValue(), FeatureType.Numeric,
        instanceNumber);

    // start splitting
    for (int i = 0; i < instance.numAttributes(); i++) {
      int index = instance.index(i);
      if (index == instance.classIndex())
        continue;
      double value = instance.value(index);

      if (instance.attribute(index).isNominal())
        nominalFeatureSetBuilder.appendFeature(index, value);
      else if (instance.attribute(index).isNumeric())
        numericFeatureSetBuilder.appendFeature(index, value);

      featuresInSplit++;
      // if chunk was populated, construct events and reset chunk building process
      if (featuresInSplit == maxFeaturesInSplitOption.getValue()) {
        List<FeatureSetContentEvent> newEvents = getEventsFromChunk(nominalFeatureSetBuilder,
            numericFeatureSetBuilder, chunkNumber);
        events.addAll(newEvents);
        featuresInSplit = 0;
        nominalFeatureSetBuilder.reset();
        numericFeatureSetBuilder.reset();
        chunkNumber++;
      }
    }
    // if there are information remaining, build extra event list
    if (featuresInSplit > 0) {
      List<FeatureSetContentEvent> newEvents = getEventsFromChunk(nominalFeatureSetBuilder,
          numericFeatureSetBuilder, chunkNumber);
      events.addAll(newEvents);
    }
    return events;
  }

  /**
   * Build feature set content events from computed chunk
   */
  private List<FeatureSetContentEvent> getEventsFromChunk(FeatureSetBuilder nominalBuilder,
      FeatureSetBuilder numericBuilder, int chunkNumber) {
    List<FeatureSetContentEvent> events = new LinkedList<>();
    FeatureSet nominalSet = new FeatureSet(nominalBuilder);
    FeatureSet numericSet = new FeatureSet(numericBuilder);
    for (int rankingIdentifier : rankingIdentifiers) {
      FeatureSetContentEvent event = new FeatureSetContentEvent(
          nominalSet, numericSet, rankingIdentifier, chunkNumber);
      events.add(event);
    }
    return events;
  }

  @Override
  public void setRankingIdentifiers(Collection<Integer> rankingIdentifiers) {
    this.rankingIdentifiers = rankingIdentifiers;
  }

  @Override
  public long getInstanceNumber() {
    return instanceNumber;
  }
}
