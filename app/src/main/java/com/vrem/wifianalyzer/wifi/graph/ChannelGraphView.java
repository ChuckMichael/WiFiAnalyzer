/*
 *    Copyright (C) 2015 - 2016 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.vrem.wifianalyzer.wifi.graph;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.vrem.wifianalyzer.MainContext;
import com.vrem.wifianalyzer.R;
import com.vrem.wifianalyzer.wifi.band.WiFiBand;
import com.vrem.wifianalyzer.wifi.band.WiFiChannels;
import com.vrem.wifianalyzer.wifi.graph.color.GraphColor;
import com.vrem.wifianalyzer.wifi.graph.color.GraphColors;
import com.vrem.wifianalyzer.wifi.model.WiFiData;
import com.vrem.wifianalyzer.wifi.model.WiFiDetail;
import com.vrem.wifianalyzer.wifi.model.WiFiSignal;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

class ChannelGraphView {
    private final MainContext mainContext = MainContext.INSTANCE;

    private final WiFiBand wiFiBand;
    private final GraphView graphView;
    private final Map<WiFiDetail, ChannelGraphSeries<DataPoint>> seriesMap;
    private final GraphViewUtils graphViewUtils;

    private ChannelGraphView(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources, @NonNull WiFiBand wiFiBand) {
        this.wiFiBand = wiFiBand;
        this.graphView = makeGraphView(graphViewBuilder, resources, this.wiFiBand);
        this.seriesMap = new TreeMap<>();
        this.graphViewUtils = new GraphViewUtils(graphView, seriesMap, mainContext.getSettings().getChannelGraphLegend());
        addDefaultsSeries(graphView, wiFiBand);
    }

    static ChannelGraphView make2(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources) {
        return new ChannelGraphView(graphViewBuilder, resources, WiFiBand.GHZ_2);
    }

    static ChannelGraphView make5(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources) {
        return new ChannelGraphView(graphViewBuilder, resources, WiFiBand.GHZ_5);
    }

    private GraphView makeGraphView(@NonNull GraphViewBuilder graphViewBuilder, @NonNull Resources resources, @NonNull WiFiBand wiFiBand) {
        return graphViewBuilder
                .setLabelFormatter(new ChannelAxisLabel(wiFiBand))
                .setVerticalTitle(resources.getString(R.string.graph_axis_y))
                .setHorizontalTitle(resources.getString(R.string.graph_channel_axis_x))
                .setWiFiBand(wiFiBand)
                .build();
    }

    void update(@NonNull WiFiData wiFiData) {
        Set<WiFiDetail> newSeries = new TreeSet<>();
        for (WiFiDetail wiFiDetail : wiFiData.getWiFiDetails(wiFiBand, mainContext.getSettings().getSortBy())) {
            addData(newSeries, wiFiDetail);
        }
        graphViewUtils.updateSeries(newSeries);
        graphViewUtils.updateLegend(mainContext.getSettings().getChannelGraphLegend());
        graphViewUtils.setVisibility(wiFiBand);
    }

    private void addData(@NonNull Set<WiFiDetail> newSeries, @NonNull WiFiDetail wiFiDetail) {
        newSeries.add(wiFiDetail);
        ChannelGraphSeries<DataPoint> series = seriesMap.get(wiFiDetail);
        if (series == null) {
            series = new ChannelGraphSeries<>(createDataPoints(wiFiDetail));
            setSeriesOptions(series, wiFiDetail);
            graphView.addSeries(series);
            seriesMap.put(wiFiDetail, series);
        } else {
            series.resetData(createDataPoints(wiFiDetail));
        }
    }

    private void setSeriesOptions(@NonNull ChannelGraphSeries<DataPoint> series, @NonNull WiFiDetail wiFiDetail) {
        GraphColor graphColor = graphViewUtils.getColor();
        series.setColor(graphColor.getPrimary());
        series.setBackgroundColor(graphColor.getBackground());
        series.setTitle(graphViewUtils.getTitle(wiFiDetail));
        graphViewUtils.setOnDataPointTapListener(series);
    }

    private DataPoint[] createDataPoints(@NonNull WiFiDetail wiFiDetail) {
        int frequencySpread = wiFiBand.getWiFiChannels().getFrequencySpread();
        WiFiSignal wiFiSignal = wiFiDetail.getWiFiSignal();
        int frequency = wiFiSignal.getFrequency();
        int frequencyStart = wiFiSignal.getFrequencyStart();
        int frequencyEnd = wiFiSignal.getFrequencyEnd();
        int level = wiFiSignal.getLevel();
        return new DataPoint[]{
                new DataPoint(frequencyStart, GraphViewBuilder.MIN_Y),
                new DataPoint(frequencyStart + frequencySpread, level),
                new DataPoint(frequency, level),
                new DataPoint(frequencyEnd - frequencySpread, level),
                new DataPoint(frequencyEnd, GraphViewBuilder.MIN_Y)
        };
    }

    private void addDefaultsSeries(@NonNull GraphView graphView, @NonNull WiFiBand wiFiBand) {
        WiFiChannels wiFiChannels = wiFiBand.getWiFiChannels();
        int frequencyOffset = wiFiBand.getWiFiChannels().getFrequencyOffset();
        DataPoint[] dataPoints = new DataPoint[]{
                new DataPoint(wiFiChannels.getWiFiChannelFirst().getFrequency() - frequencyOffset, GraphViewBuilder.MIN_Y),
                new DataPoint(wiFiChannels.getWiFiChannelLast().getFrequency() + frequencyOffset, GraphViewBuilder.MIN_Y)
        };

        ChannelGraphSeries<DataPoint> series = new ChannelGraphSeries<>(dataPoints);
        series.setColor(GraphColors.TRANSPARENT.getPrimary());
        series.setThickness(0);
        graphView.addSeries(series);
    }

    private boolean is5GHZ() {
        return WiFiBand.GHZ_5.equals(wiFiBand);
    }
}
