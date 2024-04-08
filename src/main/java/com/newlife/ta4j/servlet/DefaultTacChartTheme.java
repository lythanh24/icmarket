package com.newlife.ta4j.servlet;

import static org.jfree.chart.StandardChartTheme.createDarknessTheme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import javax.swing.UIManager;

import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.ui.RectangleInsets;

import de.sjwimmer.ta4jchart.chartbuilder.renderer.TacChartTheme;

public class DefaultTacChartTheme implements TacChartTheme {

    public DefaultTacChartTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void apply(JFreeChart chart) {
        /**
    	ChartTheme darknessTheme = createDarknessTheme();
        darknessTheme.apply(chart);
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(UIManager.getColor("Panel.background"));
        chart.getLegend().setBackgroundPaint(UIManager.getColor("Panel.background"));
        applyChart(chart);
        **/
        
        String fontName = "Lucida Sans";
        
        StandardChartTheme theme = (StandardChartTheme)org.jfree.chart.StandardChartTheme.createJFreeTheme();

        theme.setTitlePaint( Color.decode( "#4572a7" ) );
        theme.setLargeFont( new Font(fontName,Font.PLAIN, 16) ); //title
        theme.setLargeFont( new Font(fontName,Font.BOLD, 15)); //axis-title
        theme.setRegularFont( new Font(fontName,Font.PLAIN, 11));
        theme.setRangeGridlinePaint( Color.decode("#C0C0C0"));
        theme.setPlotBackgroundPaint( Color.white );
        theme.setChartBackgroundPaint( Color.white );
        theme.setGridBandPaint( Color.red );
        //theme.setAxisOffset( new RectangleInsets(0,0,0,0) );
        theme.setBarPainter(new StandardBarPainter());
        theme.setAxisLabelPaint( Color.decode("#666666")  );
        
        theme.apply( chart );
        
        chart.getXYPlot().setOutlineVisible( false );
        chart.setTextAntiAlias( true );
        chart.setAntiAlias( true );
        chart.setBorderVisible(false);
    }
}
