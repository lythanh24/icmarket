package com.newlife.ta4j.servlet;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;


import de.sjwimmer.ta4jchart.chartbuilder.ChartType;
import de.sjwimmer.ta4jchart.chartbuilder.IndicatorConfiguration;
import de.sjwimmer.ta4jchart.chartbuilder.PlotType;
import de.sjwimmer.ta4jchart.chartbuilder.TacChart;
import de.sjwimmer.ta4jchart.chartbuilder.converter.BarSeriesConverter;
import de.sjwimmer.ta4jchart.chartbuilder.converter.BarSeriesConverterImpl;
import de.sjwimmer.ta4jchart.chartbuilder.converter.IndicatorToBarDataConverter;
import de.sjwimmer.ta4jchart.chartbuilder.converter.IndicatorToBarDataConverterImpl;
import de.sjwimmer.ta4jchart.chartbuilder.converter.IndicatorToTimeSeriesConverter;
import de.sjwimmer.ta4jchart.chartbuilder.converter.IndicatorToTimeSeriesConverterImpl;
import de.sjwimmer.ta4jchart.chartbuilder.converter.TacBarDataset;
import de.sjwimmer.ta4jchart.chartbuilder.data.TacDataTableModel;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.TacBarRenderer;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.TacCandlestickRenderer;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.TacChartTheme;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.Theme;

public class TacChartBuilder {

	private final TacChartTheme theme;
	private final BarSeries barSeries;
	private TradingRecord tradingRecord;
	private final BarSeriesConverter barSeriesConverter;
	private final IndicatorToTimeSeriesConverter indicatorToTimeSeriesConverter;
	private final IndicatorToBarDataConverter indicatorToBarDataConverter;
	private final JFreeChart chart;

	public JFreeChart getChart() {
		return chart;
	}

	private final TacDataTableModel dataTableModel = new TacDataTableModel();

	private int overlayIds = 2; // 0 = ohlcv data, 1 = volume data

	public static TacChartBuilder of(BarSeries barSeries) {
		return of(barSeries, Theme.LIGHT);
	}

	public static TacChartBuilder of(BarSeries barSeries, Theme theme) {
		return new TacChartBuilder(barSeries, theme);
	}

	private TacChartBuilder(BarSeries barSeries, Theme theme) {
		this(barSeries, new BarSeriesConverterImpl(), new IndicatorToTimeSeriesConverterImpl(), new IndicatorToBarDataConverterImpl(), theme);
	}
	
	private TacChartBuilder(BarSeries barSeries, BarSeriesConverter barseriesPlotter, IndicatorToTimeSeriesConverter indicatorConverter, IndicatorToBarDataConverter indicatorToBarDataConverter, Theme theme) {
		this.theme = new DefaultTacChartTheme();
		this.barSeriesConverter = barseriesPlotter;
		this.indicatorToTimeSeriesConverter = indicatorConverter;
		this.indicatorToBarDataConverter = indicatorToBarDataConverter;
		this.barSeries = barSeries;
		this.chart = createCandlestickChart(this.barSeries);
	}

	/**
	 * Builds the chart
	 * @return a JPanel holding all ta4j-charting elements
	 */
	public TacChart build() {
		return new TacChart(chart, barSeries, dataTableModel, tradingRecord);
	}

	private static TimeSeriesCollection createAdditionalDataset(BarSeries series) {
        ClosePriceIndicator indicator = new ClosePriceIndicator(series);
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries("Btc price");
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            chartTimeSeries.add(new Second(new Date(bar.getEndTime().toEpochSecond() * 1000)),
                    indicator.getValue(i).doubleValue());
        }
        dataset.addSeries(chartTimeSeries);
        return dataset;
    }

	private JFreeChart createCandlestickChart(final BarSeries series) {
		TimeSeriesCollection xyDataset = createAdditionalDataset(series);        
		final String seriesName = series.getName();
		final ValueAxis timeAxis = new DateAxis("Time");
		final NumberAxis valueAxis = new NumberAxis("Price/Value");
		final TacCandlestickRenderer candlestickRenderer = new TacCandlestickRenderer();
		final DefaultHighLowDataset barSeriesData = this.barSeriesConverter.convert(series);
		final XYPlot plot = new XYPlot(barSeriesData, null, valueAxis, candlestickRenderer);
		setPlotTheme(plot);
		// My new code here, write here if not run so delete.
		plot.setDataset(xyDataset);
		plot.setRangeGridlinePaint(Color.lightGray);
        plot.setBackgroundPaint(Color.white);
        // end new code
		final CombinedDomainXYPlot combinedDomainPlot = new CombinedDomainXYPlot(timeAxis);

		combinedDomainPlot.add(plot,10);
		valueAxis.setAutoRangeIncludesZero(false);
		candlestickRenderer.setAutoWidthMethod(1);
		candlestickRenderer.setDrawVolume(false);
		//candlestickRenderer.setDefaultItemLabelsVisible(false);
		
		final JFreeChart chart = new JFreeChart(seriesName, JFreeChart.DEFAULT_TITLE_FONT,
				combinedDomainPlot, true);
		theme.apply(chart);
		dataTableModel.addEntries(barSeriesData);
		
		return chart;
	}

	/**
	 * Adds an indicator to the chart. The indicator can be configured with help of the {@link IndicatorConfiguration}
	 * @param indicatorConfigurationBuilder the indicatorConfiguration with the {@link Indicator}ndicator
	 * @return the {@link TacChartBuilder builder}
	 */
	public TacChartBuilder withIndicator(IndicatorConfiguration.Builder<?> indicatorConfigurationBuilder) {
		final CombinedDomainXYPlot combinedDomainPlot = (CombinedDomainXYPlot) this.chart.getPlot();
		final IndicatorConfiguration<?> indicatorConfiguration = indicatorConfigurationBuilder.build();
		final Indicator<?> indicator = indicatorConfiguration.getIndicator();
		final PlotType plotType = indicatorConfiguration.getPlotType();
		final ChartType chartType = indicatorConfiguration.getChartType();
		final String name = indicatorConfiguration.getName();
		final boolean inDataTable = indicatorConfiguration.isAddToDataTable();

		if(plotType == PlotType.OVERLAY) {
			if(chartType == ChartType.LINE) {
				final int counter = overlayIds++;
				final TimeSeriesCollection timeSeriesCollection = this.indicatorToTimeSeriesConverter.convert(indicator, name);
				final XYLineAndShapeRenderer renderer = createLineRenderer(indicatorConfiguration);
				final XYPlot candlestickPlot = (XYPlot) combinedDomainPlot.getSubplots().get(0);
				setPlotTheme(candlestickPlot);
				candlestickPlot.setRenderer(counter, renderer);
				candlestickPlot.setDataset(counter, timeSeriesCollection);
				if (inDataTable) {
					this.dataTableModel.addEntries(timeSeriesCollection);
				}
			} else if(chartType == ChartType.BAR) {
				final int counter = overlayIds++;
				final TacBarDataset barDataset = indicatorToBarDataConverter.convert(indicator, name);
				final TacBarRenderer barRenderer = createBarRenderer(indicatorConfiguration);
				final XYPlot candlestickPlot = (XYPlot) combinedDomainPlot.getSubplots().get(0);
				setPlotTheme(candlestickPlot);
				candlestickPlot.setRenderer(counter, barRenderer);
				candlestickPlot.setDataset(counter, barDataset);
				if(inDataTable) {
					this.dataTableModel.addEntries(barDataset);
				}
			}
		} else if (plotType == PlotType.SUBPLOT) {
			if(chartType == ChartType.BAR) {
				final TacBarDataset barDataset = indicatorToBarDataConverter.convert(indicator, name);
				final NumberAxis valueAxis = new NumberAxis(name);
				final TacBarRenderer barRenderer = createBarRenderer(indicatorConfiguration);
				final XYPlot barPlot = new XYPlot(barDataset, null, valueAxis, barRenderer);
				setPlotTheme(barPlot);
				valueAxis.setLabel("");
				combinedDomainPlot.add(barPlot, 1);
				if (inDataTable) {
					this.dataTableModel.addEntries(barDataset);
				}
			} else if (chartType == ChartType.LINE) {
				final TimeSeriesCollection timeSeriesCollection = this.indicatorToTimeSeriesConverter.convert(indicator, name);
				final XYLineAndShapeRenderer renderer = createLineRenderer(indicatorConfiguration);
				final NumberAxis valueAxis = new NumberAxis(name);
				final XYPlot linePlot = new XYPlot(timeSeriesCollection, null, valueAxis, renderer);
				setPlotTheme(linePlot);
				valueAxis.setLabel("");
				valueAxis.setAutoRangeIncludesZero(false);
				if (inDataTable) {
					this.dataTableModel.addEntries(timeSeriesCollection);
				}
				combinedDomainPlot.add(linePlot, 1);
			}
		}
		return this;
	}

	private void setPlotTheme(XYPlot plot) {
		final Color labelColor = UIManager.getColor("Label.foreground");
		plot.setBackgroundPaint(UIManager.getColor("Panel.background"));
		if(plot.getRangeAxis() != null) {
			plot.getRangeAxis().setTickLabelPaint(labelColor);
			plot.getRangeAxis().setLabelPaint(labelColor);
		}
		if(plot.getDomainAxis() != null) {
			plot.getDomainAxis().setTickLabelPaint(labelColor);
			plot.getDomainAxis().setLabelPaint(labelColor);
		}
	}

	private TacBarRenderer createBarRenderer(IndicatorConfiguration<?> indicatorConfiguration) {
		return new TacBarRenderer(indicatorConfiguration.getColor());
	}

	private XYLineAndShapeRenderer createLineRenderer(IndicatorConfiguration<?> indicatorConfiguration) {
		XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
		lineRenderer.setSeriesShape(0, indicatorConfiguration.getShape());
		lineRenderer.setSeriesPaint(0, indicatorConfiguration.getColor());
		return lineRenderer;
	}


	public TacChartBuilder withTradingRecord(TradingRecord tradingRecord) {
		this.tradingRecord = tradingRecord;
		return this;
	}

	public void buildAndShow(String title) {
		javax.swing.SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame(title);
			frame.setLayout(new BorderLayout());
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(this.build());
			frame.pack();
			frame.setVisible(true);
		});
	}

	/**
	 * Builds and shows the chart panel in a JFrame with BorderLayout
	 */
	public void buildAndShow(){
		buildAndShow("Ta4j charting");
	}
}
