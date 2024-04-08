/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2023 Ta4j Organization & respective
 * authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.newlife.ta4j.servlet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.axis.Timeline;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import com.newlife.loader.JiraBarLoader;


/**
 * This class builds a graphical chart showing values from indicators.
 */
public class ATRAndHistogramToChart extends HttpServlet {
	
	private static final int MACD_SHORT_TIMEFRAME = 9;
	private static final int EMA_SHORT_TIMEFRAME = 13;
	private static final int EMA_LONG_TIMEFRAME = 26;
    private static final int VOLUME_DATASET_INDEX = 1;
    private static CombinedDomainXYPlot combinedPlot;
    private static JFreeChart combinedChart;
    static DateAxis xAxis = new DateAxis("Time");
    static Stroke dashedThinLineStyle = new BasicStroke(0.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
            new float[] { 8.0f, 4.0f }, 0.0f);
    
    static BarSeries series;

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	String pid = Optional.ofNullable(req.getParameter("pid")).orElse("HSX");
    	String symbol = Optional.ofNullable(req.getParameter("symbol")).orElse("MWG");    	
    	
    	if(symbol.equalsIgnoreCase("VN30")) {
    		String createdAfterStr = Optional.ofNullable(req.getParameter("created")).orElse("25/12/2022");
    		Date createdAfter = null;
    		try {
				createdAfter = new SimpleDateFormat("dd/MM/yyyy").parse(createdAfterStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}  
    		series = JiraBarLoader.loadVN30Series(pid, symbol, createdAfter);
    	} else {
    		series = JiraBarLoader.loadSeries(pid, symbol);
    	}
    	
    	
    	JFreeChart chart =createIndicatorsForChart(series);
    	
    	ServletOutputStream out = resp.getOutputStream();
	    resp.setContentType("image/png");
	    ChartUtils.writeChartAsPNG(out, chart, 2000, 1600);
    }
	
    	

    /**
     * Builds a JFreeChart time series from a Ta4j bar series and an indicator.
     *
     * @param barSeries the ta4j bar series
     * @param indicator the indicator
     * @param name      the name of the chart time series
     * @return the JFreeChart time series
     */
    private static org.jfree.data.time.TimeSeries buildChartBarSeries(BarSeries barSeries, Indicator<Num> indicator,
            String name) {
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
        for (int i = 0; i < barSeries.getBarCount(); i++) {
            Bar bar = barSeries.getBar(i);
            chartTimeSeries.add(new Day(Date.from(bar.getEndTime().toInstant())), indicator.getValue(i).doubleValue());
        }
        return chartTimeSeries;
    }
    
    /**
     * Displays a chart in a frame.
     *
     * @param ohlcDataset
     * @param xyDataset
     * @param histogramSeries
     * @return 
     */
    private static JFreeChart displayChart(XYDataset ohlcDataset, XYDataset xyDataset, XYDataset histogramSeries, XYDataset histogramBar) {
        /*
         * Create the chart
         */
        CandlestickRenderer renderer = new CandlestickRenderer();
        XYPlot pricePlot = new XYPlot(ohlcDataset, xAxis, new NumberAxis("Price"), renderer);
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST);
        renderer.setDrawVolume(false);
        // Remove weekend on chart
        final Timeline newTimeline = SegmentedTimeline.newMondayThroughFridayTimeline();
        xAxis.setTimeline(newTimeline);
        
        // volume dataset
        pricePlot.setDataset(VOLUME_DATASET_INDEX, xyDataset);
        pricePlot.mapDatasetToRangeAxis(VOLUME_DATASET_INDEX, 0);
        // plot.setDomainAxis( xAxis );
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
        renderer2.setSeriesPaint(VOLUME_DATASET_INDEX, Color.blue);
        pricePlot.setRenderer(VOLUME_DATASET_INDEX, renderer2);
        // Misc
        pricePlot.setRangeGridlinePaint(Color.lightGray);
        pricePlot.setBackgroundPaint(Color.white);
        NumberAxis numberAxis = (NumberAxis) pricePlot.getRangeAxis();
        pricePlot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        renderer.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_SMALLEST);
        // Misc
        pricePlot.setRangeGridlinePaint(Color.lightGray);
        pricePlot.setBackgroundPaint(Color.white);
        numberAxis.setAutoRangeIncludesZero(true);
        pricePlot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        
        // create MACD and Histogram ...
        final XYItemRenderer macdRenderer = new StandardXYItemRenderer();
        final NumberAxis macdRangeAxis = new NumberAxis("MACD");
        macdRangeAxis.setAutoRangeIncludesZero(true);
        final XYPlot macdPlot = new XYPlot(histogramSeries, null, macdRangeAxis, macdRenderer);
        macdPlot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
        
        NumberAxis rangeAxis2 = new NumberAxis("Volume");
        rangeAxis2.setUpperMargin(1.00);  // to leave room for price line
        macdPlot.setRangeAxis(1, rangeAxis2);
        macdPlot.setDataset(1, histogramBar);
        macdPlot.setRangeAxis(1, rangeAxis2);
        macdPlot.mapDatasetToRangeAxis(1, 1);
        XYBarRenderer barRenderer = new XYBarRenderer();
        barRenderer.setDrawBarOutline(false);
        
        macdPlot.setRenderer(1, barRenderer);
        barRenderer.setShadowVisible(false);
        barRenderer.setBarPainter(new StandardXYBarPainter());
        
        // combinedPlot
        combinedPlot = new CombinedDomainXYPlot(xAxis); // DateAxis
        combinedPlot.setGap(10.0);
        // combinedPlot.setDomainAxis( xAxis );
        combinedPlot.setBackgroundPaint(Color.LIGHT_GRAY);
        combinedPlot.setDomainGridlinePaint(Color.GRAY);
        combinedPlot.setRangeGridlinePaint(Color.GRAY);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);
        combinedPlot.add(pricePlot, 60);
        combinedPlot.add(macdPlot, 20);
        
        

        // Now create the chart that contains the combinedPlot
        combinedChart = new JFreeChart("StockChart", null, combinedPlot, true);
        combinedChart.setBackgroundPaint(Color.LIGHT_GRAY);
        

        return combinedChart;
        
    }
    
    public static JFreeChart createIndicatorsForChart(BarSeries series) {
    	        
        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
        System.out.println("Close prices [5]"+ closePrices.getValue(5));
        
        // For VN30 volume is climax
        VolumeIndicator volume = new VolumeIndicator(series);
        TransformIndicator doubleVolume = TransformIndicator.multiply(volume, 5.0);
        TransformIndicator tripleVolume = TransformIndicator.plus(doubleVolume, 800.0);
        EMAIndicator emaClimax = new EMAIndicator(tripleVolume, 9);
        
        EMAIndicator longEma = new EMAIndicator(closePrices, EMA_LONG_TIMEFRAME);
        
        // Getting the first ATRIndicator
        ATRIndicator aTR = new ATRIndicator(series, 14);
        
        // Getting 2ATR
        TransformIndicator doubleATR = TransformIndicator.multiply(aTR, 2.0);
        
        // Getting 2ATR
        TransformIndicator trippleATR = TransformIndicator.multiply(aTR, 3.0); 
        
        // Getting ATR Channel
        CombineIndicator ATRBand = CombineIndicator.plus(aTR, longEma); 
        CombineIndicator doubleATRBand = CombineIndicator.plus(doubleATR, longEma);
        CombineIndicator trippleATRBand = CombineIndicator.plus(trippleATR, longEma);

        // Getting ATR Channel
        CombineIndicator ATRDownBand = CombineIndicator.minus(longEma, aTR); 
        CombineIndicator doubleDownATRBand = CombineIndicator.minus(longEma, doubleATR);
        CombineIndicator trippleDownATRBand = CombineIndicator.minus(longEma, trippleATR);
        
        MACDIndicator macd = new MACDIndicator(closePrices, EMA_SHORT_TIMEFRAME, EMA_LONG_TIMEFRAME);
        TransformIndicator plusMacd = TransformIndicator.multiply(macd, 1000.0);
        EMAIndicator emaMacd = new EMAIndicator(plusMacd, MACD_SHORT_TIMEFRAME);        
        CombineIndicator histogram = CombineIndicator.minus(plusMacd, emaMacd);
        
        System.out.println("MACD [5]"+ macd.getValue(5).toString() + ", plusMacd: " + plusMacd.getValue(5) 
        	+ " emaMacd: "+emaMacd.getValue(5).toString() 
        	+ " histogram: "+histogram.getValue(5).toString());
        /*
         * Create the OHLC dataset from the data series
         */
        OHLCDataset ohlcDataset = CandlestickChart.createOHLCDataset(series);
        /*
         * Create volume dataset
         */
        // TimeSeriesCollection xyDataset = CandlestickChartWithChopIndicator.createAdditionalDataset(series);
        TimeSeriesCollection xyDataset = new TimeSeriesCollection();
        xyDataset.addSeries(buildChartBarSeries(series, macd, "MACD"));
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(buildChartBarSeries(series, longEma, "+EMA26"));
        dataset.addSeries(buildChartBarSeries(series, ATRBand, "+1ATR"));
        dataset.addSeries(buildChartBarSeries(series, doubleATRBand, "+2ATR"));     
        dataset.addSeries(buildChartBarSeries(series, trippleATRBand, "+3ATR"));
        dataset.addSeries(buildChartBarSeries(series, ATRDownBand, "-1ATR"));
        dataset.addSeries(buildChartBarSeries(series, doubleDownATRBand, "-2ATR"));
        dataset.addSeries(buildChartBarSeries(series, trippleDownATRBand, "-3ATR"));
        if("VN30".equalsIgnoreCase(series.getName())) {        	
        	dataset.addSeries(buildChartBarSeries(series, tripleVolume, "Climax"));
        	dataset.addSeries(buildChartBarSeries(series, emaClimax, "EMA(Climax,9)"));
        }

        /*
         * add the MACD&Histogram Indicator
         */
        TimeSeriesCollection histogramSeries = new TimeSeriesCollection();
        histogramSeries.addSeries(buildChartBarSeries(series, plusMacd, "MACD"));
        histogramSeries.addSeries(buildChartBarSeries(series, emaMacd, "Signal macd"));
        
        
        TimeSeriesCollection histogramBar = new TimeSeriesCollection();
        histogramBar.addSeries(buildChartBarSeries(series, histogram, "Histogram"));
        
        /*
         * Display the chart
         */
        return displayChart(ohlcDataset, dataset, histogramSeries, histogramBar);
    }

}
