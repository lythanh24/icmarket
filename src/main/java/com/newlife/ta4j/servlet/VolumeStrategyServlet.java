package com.newlife.ta4j.servlet;

import static de.sjwimmer.ta4jchart.chartbuilder.IndicatorConfiguration.Builder.of;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jfree.chart.JFreeChart;
import org.jfree.data.general.SeriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.AnalysisCriterion.PositionFilter;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.Returns;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.criteria.PositionsRatioCriterion;
import org.ta4j.core.criteria.ReturnOverMaxDrawdownCriterion;
import org.ta4j.core.criteria.VersusEnterAndHoldCriterion;
import org.ta4j.core.criteria.pnl.ReturnCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator;
import org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator.ConvergenceDivergenceType;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.BooleanIndicatorRule;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.IsRisingRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.order.SortOrder;
import com.newlife.loader.JiraBarLoader;

import de.sjwimmer.ta4jchart.chartbuilder.ChartType;
import de.sjwimmer.ta4jchart.chartbuilder.PlotType;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.Theme;


public class VolumeStrategyServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(MovingMomentumStrategy.class);

	ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
	CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
	IssueService issueService = ComponentAccessor.getIssueService();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html");
		resp.getWriter().write("<html>"
				+ "<head>"
				+ "    <title>Stock Analystic — Select Strategy to Run</title>"
				+ "    <meta name=\"decorator\" content=\"atl.general\">"
				+ "</head>"
				+ "<body class=\"page-type-admin\"><div class=\"content-container\" id=\"issue-crud-container\">");
		PrintWriter writer = resp.getWriter();

		BarSeries series = new BaseBarSeries();

		// get project id (pid) in request param
		String pid = req.getParameter("pid");
		if (null != pid) {
			writer.println("Yes, found 'pid'. Searching for Component in " + pid + " project...<br><br>");

		} else {
			pid = "HSX";
			writer.println("Not found 'pid' project, Seach for default project " + pid + "...<br><br>");
		}

		// get component name (component) in request param
		String component = req.getParameter("component");
		if (null != component) {
			writer.println(
					"Yes, found 'component' name. Searching for company: " + component + " project...<br><br>");

		} else {
			component = "MWG";
			writer.println("Not set 'component', Seach for default company " + component + "...<br><br>");
		}
		
		String volume = req.getParameter("volume");
		if (null != volume) {
			writer.println(
					"Yes, found 'volume' = " + volume + " ...<br><br>");

		} else {
			volume = "1000000";
			writer.println("Not set 'volume', Set for default volume: " + volume + "...<br><br>");
		}

		writer.println("Searching for issues in HSX...<br><br>");

		try {
			
			writer.println("find stock by Volume Strategy in HSX...<br><br>");			
			// Run Vn30 Symbol in HOSE
			Collection<Strategy> strategyLst = JiraBarLoader.findStockByVolumeStrategy(pid, volume);
			Iterator<Strategy> iterator = strategyLst.iterator();
			Strategy strategy;
			
			while(iterator.hasNext()) {
				strategy = iterator.next();
				writer.print(strategy.getName()+";");
			}
			writer.println("Done strategy checking, total good case:"+strategyLst.size()+" ----- <br><br>");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println("<br>DONE<br><br>");

		writer.println("Searching for issues in DEMO project, assigned to current user...<br><br>");

		writer.println("<br>DONE<br><br>");

		writer.println("</div></body></html>");
	}

	private void runCDIndicator(BarSeries series) {
		int timePeriod = 100;
		double param1 = 10; // will be divided by 100
		double param2 = 10; // will be divided by 100
		double aHundreds = 100.0;
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		Rule buyingRule;
		Rule sellingRule;

		VolumeIndicator volume = new VolumeIndicator(series);
		ConvergenceDivergenceIndicator.ConvergenceDivergenceType posDivType = ConvergenceDivergenceType.positiveDivergent;
		ConvergenceDivergenceIndicator.ConvergenceDivergenceType negDivType = ConvergenceDivergenceType.negativeDivergent;
		ConvergenceDivergenceIndicator posDiv = new ConvergenceDivergenceIndicator(closePrice, volume, timePeriod,
				posDivType, param1 / aHundreds, param2 / aHundreds);
		ConvergenceDivergenceIndicator negDiv = new ConvergenceDivergenceIndicator(closePrice, volume, timePeriod,
				negDivType, param1 / aHundreds, param2 / aHundreds);

		buyingRule = new BooleanIndicatorRule(posDiv);
		sellingRule = new BooleanIndicatorRule(negDiv);

		Strategy strategy = new BaseStrategy(buyingRule, sellingRule);
		
	}

	public static JFreeChart buildTACChart(BarSeries barSeries) {

		final VolumeIndicator volume = new VolumeIndicator(barSeries);
		final ParabolicSarIndicator parabolicSar = new ParabolicSarIndicator(barSeries);
		final ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
		final EMAIndicator longEma = new EMAIndicator(closePrice, 12);
		final EMAIndicator shortEma = new EMAIndicator(closePrice, 4);
		final CrossedDownIndicatorRule exit = new CrossedDownIndicatorRule(shortEma, longEma);
		final CrossedUpIndicatorRule entry = new CrossedUpIndicatorRule(shortEma, longEma);

		final Strategy strategy = new BaseStrategy(entry, exit);
		final TradingRecord tradingRecord = new BarSeriesManager(barSeries).run(strategy);

		final Returns returns = new Returns(barSeries, tradingRecord, Returns.ReturnType.ARITHMETIC);

		// 2 Use the ChartBuilder to create a plot with barSeries, indicators and
		// trading record
		JFreeChart chart = TacChartBuilder.of(barSeries, Theme.LIGHT)
				.withIndicator(
						of(shortEma)
						.name("Short Ema")
						.color(Color.BLUE)) // default: ChartType.LINE,
																					// PlotType.OVERLAY
				.withIndicator(
						of(volume).name("Volume")
						.plotType(PlotType.SUBPLOT).chartType(ChartType.BAR).color(Color.BLUE))
				.withIndicator(of(parabolicSar) // default name = toString()
						.plotType(PlotType.OVERLAY).chartType(ChartType.LINE).color(Color.MAGENTA))
				.withIndicator(of(longEma).name("Long Ema").plotType(PlotType.SUBPLOT).chartType(ChartType.LINE)) // random
																													// color
				.withIndicator(of(returns).name("Returns").plotType(PlotType.SUBPLOT).color(Color.BLACK) // default:
																											// ChartType.LINE
						.notInTable()) // do not show entries in data table
				.withTradingRecord(tradingRecord).getChart();

		return chart;

	}
	
	public Collection<Strategy> runHOSEMoritoring(String pid, int numberOfSymbol, PrintWriter writer) {
		// Run list of Symbol in HOSE project
		Collection<BarSeries> stockList = buildBarSeries(pid, numberOfSymbol);
		Collection<Strategy> strategyList = new ArrayList<Strategy>();
		
		log.info("Start running for "+stockList.size() + "Symbol");
		Iterator<BarSeries> iterator = stockList.iterator();
		BarSeries series;
		
		int yearHL=0;
		
		while(iterator.hasNext()) {
			try {
				series = iterator.next();				
		        
				String symbol = series.getName();
				
				double symbolIsLowest = at52WLow(series);
				if(symbolIsLowest > 0.0) {
					writer.println("LOWEST SYMBOL FOUND: "+ symbol + ":"+ symbolIsLowest+"...<br><br>");
				}
				Strategy strategy = buildMACDStrategy(series);
				
				int endIndex = series.getEndIndex();
				log.info("Checking strategy ("+symbol+") endIndex " + endIndex +"; "+ strategy.getName());
				
				if (strategy.shouldEnter(endIndex)) {
					strategyList.add(strategy);
					log.info("=========== FOUND A SIGNAL SHOULD ENTER=========");
					log.info("====="+symbol+" endIndex " + endIndex +"; "+ strategy.getName());
					log.info("===================================================");
				}
				
				// Get High-Low indicator for current series
				int currentSymbolHL = yearHL(series);
				
				yearHL = currentSymbolHL + yearHL;
				
				// Run extrema strategy to file stock near lowest/ highest value
				Strategy extremaStrategy = buildExtremaStrategy(series);
				if (extremaStrategy.shouldExit(endIndex)) {
					strategyList.add(extremaStrategy);
					log.info("=========== FOUND A extremaStrategy SIGNAL SHOULD ENTER=========");
					log.info("====="+symbol+" endIndex " + endIndex +"; "+ extremaStrategy.getName());
					log.info("===================================================");
				}
				

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info("=========TODAY YEAR-HIGH-LOW: " + yearHL);
		writer.println("=========TODAY YEAR-HIGH-LOW: " + yearHL+"...<br><br>");
		return strategyList;
	}
	
	public double at52WLow(BarSeries series) {
		
		String symbol = series.getName();
		int endIndex = series.getEndIndex();
		int yearBar = 260;
		if(endIndex < yearBar) {
			yearBar = endIndex;
		}
		
		LowPriceIndicator lowPrice = new LowPriceIndicator(series);
		double lastLowPrice = lowPrice.getValue(endIndex).doubleValue();
		
		log.info(symbol+" endLowPrice: "+lastLowPrice);
		
		LowestValueIndicator lowestValue = new LowestValueIndicator(lowPrice, yearBar);
		
		double lowest = lowestValue.getValue(yearBar).doubleValue();
		
		log.info(symbol+", "+yearBar+" bars lowest: "+lowest);
		
		if(lastLowPrice <= lowest) {
			log.info("------Wow----- "+symbol+" is at Lowest of Year");
			return lastLowPrice;
		}
		log.info("----------- "+symbol+" is at Normal value of Year");
		
		return 0.0;
		
	}
	
	public int yearHL(BarSeries series) {
		
		int endIndex = series.getEndIndex();
		int yearBar = 260;
		if(endIndex < yearBar) {
			yearBar = endIndex;
		}
		
		HighPriceIndicator highPrice = new HighPriceIndicator(series);
		double lastHighPrice = highPrice.getValue(endIndex).doubleValue();
		log.info("endHighPrice: "+lastHighPrice);
		
		HighestValueIndicator highestYearValue = new HighestValueIndicator(highPrice, yearBar);
		
		double highest = highestYearValue.getValue(endIndex).doubleValue();
		log.info(yearBar+" bars highest: "+highest);
		
		if(lastHighPrice >= highest) {
			log.info("------Wow----- "+series.getName()+" is at Highest of Year");
			return 1;
		}
		LowPriceIndicator lowPrice = new LowPriceIndicator(series);
		double lastLowPrice = lowPrice.getValue(endIndex).doubleValue();
		log.info("endLowPrice: "+lastLowPrice);
		
		LowestValueIndicator lowestYearValue = new LowestValueIndicator(lowPrice, yearBar);
		double lowest = lowestYearValue.getValue(endIndex).doubleValue();		
		log.info(endIndex+" bars lowest: "+lowest);
		
		double lowest0 = lowestYearValue.getValue(yearBar).doubleValue();		
		log.info(yearBar+" bars lowest: "+lowest0);
		
		double lowest1 = lowestYearValue.getValue(10).doubleValue();		
		log.info(10+" bars lowest: "+lowest1);
		
		double lowest2 = lowestYearValue.getValue(20).doubleValue();		
		log.info(20+" bars lowest: "+lowest2);
		
		if(lastLowPrice <= lowest) {
			log.info("------Wow----- "+series.getName()+" is at Lowest of Year");
			return -1;
		}
		log.info("----------- "+series.getName()+" is at Normal value of Year");
		
		return 0;
	}
	
	public static Strategy buildExtremaStrategy(BarSeries series) {
		int NB_BARS_PER_YEAR = 5*4*12;
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);

        // Getting the high price over the past year
        HighPriceIndicator highPrices = new HighPriceIndicator(series);
        HighestValueIndicator yearHighPrice = new HighestValueIndicator(highPrices, NB_BARS_PER_YEAR);
        // Getting the low price over the past year
        LowPriceIndicator lowPrices = new LowPriceIndicator(series);
        LowestValueIndicator yearLowPrice = new LowestValueIndicator(lowPrices, NB_BARS_PER_YEAR);

        // Going long if the close price goes below the low price
        TransformIndicator downYear = TransformIndicator.multiply(yearLowPrice, 1.004);
        Rule buyingRule = new UnderIndicatorRule(closePrices, downYear);

        // Going short if the close price goes above the high price
        TransformIndicator upYear = TransformIndicator.multiply(yearHighPrice, 0.996);
        Rule sellingRule = new OverIndicatorRule(closePrices, upYear);

        return new BaseStrategy(series.getName(), buyingRule, sellingRule);
    }
	
	public void findPinAndHammer(BarSeries series) {
		// Define a Rule for pin bars
        Rule pinBarRule = (index, tradingRecord) -> {
            Bar currentBar = series.getBar(index);
            // Calculate the size of the body
            Num bodySize = currentBar.getOpenPrice().minus(currentBar.getClosePrice()).abs();
            // Calculate the size of the upper shadow
            Num upperShadowSize = currentBar.getHighPrice().minus(currentBar.getOpenPrice());
            // Calculate the size of the lower shadow
            Num lowerShadowSize = currentBar.getClosePrice().minus(currentBar.getLowPrice());

            // Define the conditions for a pin bar
            return bodySize.isLessThan(upperShadowSize.multipliedBy(DoubleNum.valueOf(2)))
                && bodySize.isLessThan(lowerShadowSize.multipliedBy(DoubleNum.valueOf(2)));
        };

        // Define a Rule for hammer bars
        Rule hammerBarRule = (index, tradingRecord) -> {
            Bar currentBar = series.getBar(index);
            // Calculate the size of the body
            Num bodySize = currentBar.getClosePrice().minus(currentBar.getOpenPrice()).abs();
            // Calculate the size of the lower shadow
            Num lowerShadowSize = currentBar.getClosePrice().minus(currentBar.getLowPrice());
            // Calculate the size of the upper shadow
            Num upperShadowSize = currentBar.getHighPrice().minus(currentBar.getOpenPrice());

            // Define the conditions for a hammer bar
            return bodySize.isLessThan(upperShadowSize.multipliedBy(DoubleNum.valueOf(2)))
                && lowerShadowSize.isGreaterThan(bodySize.multipliedBy(DoubleNum.valueOf(2)));
        };

        // Iterate over the series and apply the rules to identify pin bars and hammer bars
        for (int i = 0; i < series.getBarCount(); i++) {
            if (pinBarRule.isSatisfied(i, null)) {
                System.out.println("Pin bar found at index " + i);
            }
            if (hammerBarRule.isSatisfied(i, null)) {
                System.out.println("Hammer bar found at index " + i);
            }
        }
    }


	public static Strategy buildMACDStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        int endIndex = series.getEndIndex();
        log.info("buildMACDStrategy for "+series.getName());

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        log.info("----ClosePriceIndicator[last]"+closePrice.getValue(endIndex));
        
        // The bias is bullish when the shorter-moving average moves above the longer moving average.
        // The bias is bearish when the shorter-moving average moves below the longer moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, 13);
        Rule isRisingEma13 = new IsRisingRule(shortEma, 2);
        log.info("shortEma isRising: "+ isRisingEma13.isSatisfied(endIndex));
        log.info("----EMA13[last]"+shortEma.getValue(endIndex));
        
        EMAIndicator longEma = new EMAIndicator(closePrice, 26);
        log.info("----EMA26[last]"+longEma.getValue(endIndex));

        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);
        log.info("----StochasticOscillatorK14[last]"+stochasticOscillK.getValue(endIndex));

        MACDIndicator macd = new MACDIndicator(closePrice, 13, 26);
        log.info("----MACD1326[last]"+macd.getValue(endIndex));
        
        EMAIndicator emaMacd = new EMAIndicator(macd, 9);
        log.info("----emaMacd[last]"+emaMacd.getValue(endIndex));
        
        Indicator<Num> histogram = CombineIndicator.minus(macd,emaMacd);
        log.info("----histogram[last]"+histogram.getValue(endIndex));
        
        Rule histogramIsRising = new IsRisingRule(histogram, 2);
        log.info("histogram IsRisingRule:"+histogramIsRising.isSatisfied(endIndex));
        
        // Entry rule
        Rule entryRule = histogramIsRising
        		//.and(new IsRisingRule(shortEma, 2))
        		.and(new UnderIndicatorRule(closePrice, longEma))
        		.and(new IsRisingRule(shortEma, 2)); // Trend
                //.and(new CrossedDownIndicatorRule(stochasticOscillK, DecimalNum.valueOf(20))) // Signal 1
                //.and(new UnderIndicatorRule(closePrice, longEma)); // Signal 2
                //.and(new IsRisingRule(histogram, 2)); // Signal 3
        
        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, DecimalNum.valueOf(80))) // Signal 1
                .and(new UnderIndicatorRule(closePrice, emaMacd)); // Signal 2
        
        Strategy strategy = new BaseStrategy(series.getName(), entryRule, exitRule);

        log.info("Strategy for BarSeries:" + series.getName() +"="+strategy.getName());
        
        return strategy;
        
        /**
        int endIndex = series.getEndIndex();
        if (strategy.shouldEnter(endIndex)) {
            // Entering...
            tradingRecord.enter(endIndex, newBar.getClosePrice(), DoubleNum.valueOf(10));
        } else if (strategy.shouldExit(endIndex)) {
            // Exiting...
            tradingRecord.exit(endIndex, newBar.getClosePrice(), DoubleNum.valueOf(10));
        }
        **/
        
    }

	public void runTa4j(BarSeries series) {

		Num firstClosePrice = series.getBar(0).getClosePrice();
		log.info("First close price: " + firstClosePrice.doubleValue());
		// Or within an indicator:
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		// Here is the same close price:
		// log.info(firstClosePrice.isEqual(closePrice.getValue(0))); // equal to firstClosePrice

		// Getting the simple moving average (SMA) of the close price over the last 5
		// bars
		SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
		// Here is the 5-bars-SMA value at the 42nd index
		log.info("5-bars-SMA value at the 26nd index: " + shortSma.getValue(26).doubleValue());

		// Getting a longer SMA (e.g. over the 30 last bars)
		SMAIndicator longSma = new SMAIndicator(closePrice, 30);

		// Ok, now let's building our trading rules!

		// Buying rules
		// We want to buy:
		// - if the 5-bars SMA crosses over 30-bars SMA
		// - or if the price goes below a defined price (e.g $800.00)
		Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma)
				.or(new CrossedDownIndicatorRule(closePrice, 800));

		// Selling rules
		// We want to sell:
		// - if the 5-bars SMA crosses under 30-bars SMA
		// - or if the price loses more than 3%
		// - or if the price earns more than 2%
		Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
				.or(new StopLossRule(closePrice, series.numOf(3))).or(new StopGainRule(closePrice, series.numOf(2)));

		// Running our juicy trading strategy...
		BarSeriesManager seriesManager = new BarSeriesManager(series);
		TradingRecord tradingRecord = seriesManager.run(new BaseStrategy(buyingRule, sellingRule));
		log.info("Number of positions for our strategy: " + tradingRecord.getPositionCount());

		// Analysis

		// Getting the winning positions ratio
		AnalysisCriterion winningPositionsRatio = new PositionsRatioCriterion(PositionFilter.PROFIT);
		log.info("Winning positions ratio: " + winningPositionsRatio.calculate(series, tradingRecord));
		// Getting a risk-reward ratio
		AnalysisCriterion romad = new ReturnOverMaxDrawdownCriterion();
		log.info("Return over Max Drawdown: " + romad.calculate(series, tradingRecord));

		// Total return of our strategy vs total return of a buy-and-hold strategy
		AnalysisCriterion vsBuyAndHold = new VersusEnterAndHoldCriterion(new ReturnCriterion());
		log.info("Our return vs buy-and-hold return: " + vsBuyAndHold.calculate(series, tradingRecord));

		// Building the trading strategy
		Strategy strategy = buildStrategy(series);

		// Running the strategy
		BarSeriesManager seriesManager2 = new BarSeriesManager(series);
		TradingRecord tradingRecord2 = seriesManager2.run(strategy);
		log.info("Number of positions for the strategy: " + tradingRecord2.getPositionCount());

		// Analysis
		log.info("Total return for the strategy: " + new ReturnCriterion().calculate(series, tradingRecord2));

	}

	public static BarSeries createBarSeriesFromListIssue(List<Issue> issues, String name) {

		try {
			final BarSeries barSeries = new BaseBarSeries(name);
			
			Issue issue = null;

			for (int i = 0; i < issues.size(); i++) {

				issue = (Issue) issues.get(i);

				// AAA;11.15;11.25;10.7;10.8;6690300
				String summary = issue.getSummary();
				String[] parts = summary.trim().split(";");

				Timestamp timestamp = issue.getCreated();
				// Convert to Instant
				Instant instant = timestamp.toInstant();
				// Convert to ZonedDateTime using system default time zone
				ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());

				double open = Double.parseDouble(parts[1]);
				double high = Double.parseDouble(parts[2]);
				double low = Double.parseDouble(parts[3]);
				double close = Double.parseDouble(parts[4]);
				double volume = Double.parseDouble(parts[5]);

				// log.info("day: " + zonedDateTime.toString() + " ,open: " + open + "
				// ,high: " + high + " ,low: " + low + " ,close: " + close);
				// check duplicate time
				// lastBar#beginTime + lastBar#duration <= newBar#beginTime
				if (i == 0) {
					barSeries.addBar(zonedDateTime, open, high, low, close, volume);
				} else {
					Bar lastBar = barSeries.getBar(barSeries.getEndIndex());
					boolean isNewBar = lastBar.getBeginTime().isBefore(zonedDateTime.minusHours(12));
					
					log.debug("barSeries.getEndIndex()=" + barSeries.getEndIndex());
					log.debug("lastBar.getBeginTime=" + lastBar.getBeginTime() + " VS zonedDateTime: " + zonedDateTime.toString());
					log.debug("isNewBar: " + isNewBar);
					
					if (isNewBar) {
						try {
							barSeries.addBar(zonedDateTime, open, high, low, close, volume);
						} catch (SeriesException e) {
							log.error(e.toString());
						} catch (IllegalArgumentException ex) {
							log.error(ex.toString());
						}
					}
				}
			}
			/**
			List<Bar> barDuplicates = BarSeriesUtils.findOverlappingBars(barSeries);
			for (int i = 0; i < barDuplicates.size(); i++) {
				Bar barRemove = (Bar) barDuplicates.get(i);
				log.info("=========Duplicate bar: " + barRemove.toString());
			}
			**/

			return barSeries;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param series a bar series
	 * @return an adx indicator based strategy
	 */
	public static Strategy buildStrategy(BarSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		final ClosePriceIndicator closePriceIndicator = new ClosePriceIndicator(series);
		final SMAIndicator smaIndicator = new SMAIndicator(closePriceIndicator, 50);

		final int adxBarCount = 14;
		final ADXIndicator adxIndicator = new ADXIndicator(series, adxBarCount);
		final OverIndicatorRule adxOver20Rule = new OverIndicatorRule(adxIndicator, 20);

		final PlusDIIndicator plusDIIndicator = new PlusDIIndicator(series, adxBarCount);
		final MinusDIIndicator minusDIIndicator = new MinusDIIndicator(series, adxBarCount);

		final Rule plusDICrossedUpMinusDI = new CrossedUpIndicatorRule(plusDIIndicator, minusDIIndicator);
		final Rule plusDICrossedDownMinusDI = new CrossedDownIndicatorRule(plusDIIndicator, minusDIIndicator);
		final OverIndicatorRule closePriceOverSma = new OverIndicatorRule(closePriceIndicator, smaIndicator);
		final Rule entryRule = adxOver20Rule.and(plusDICrossedUpMinusDI).and(closePriceOverSma);

		final UnderIndicatorRule closePriceUnderSma = new UnderIndicatorRule(closePriceIndicator, smaIndicator);
		final Rule exitRule = adxOver20Rule.and(plusDICrossedDownMinusDI).and(closePriceUnderSma);

		return new BaseStrategy("ADX", entryRule, exitRule, adxBarCount);
	}

	// Search for issues in DEMO project, assigned to current user
	private List<Issue> getIssuesInProject(ApplicationUser user) throws SearchException {

		JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

		builder.where().project("DEMO").and().assigneeIsCurrentUser();
		builder.orderBy().createdDate(SortOrder.ASC);
		Query query = builder.buildQuery();
		SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
		SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

		return getIssuesInSearchResults(results);

	}

	// Search for issues in multiple projects, with customer name Jobin,
	// assigned to jobin or admin
	private List<Issue> getIssuesInProjectsForCustomer(ApplicationUser user) throws SearchException {
		JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

		builder.where().project("TEST", "DEMO").and().assignee().in("jobinkk", "admin").and().customField(10000L)
				.eq("jobinkk");
		builder.orderBy().createdDate(SortOrder.ASC);
		Query query = builder.buildQuery();
		SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
		SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

		return getIssuesInSearchResults(results);
	}

	// Search for issues in multiple projects, with empty assignee or reporter
	private List<Issue> getIssuesInProjectsWithEmptyUsers(ApplicationUser user) throws SearchException {
		JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();

		builder.where().project("HOSE", "DEMO").and().sub().assigneeIsEmpty().or().reporterIsEmpty().endsub();
		builder.orderBy().createdDate(SortOrder.ASC);
		Query query = builder.buildQuery();
		SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
		SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

		return getIssuesInSearchResults(results);
	}

	// Search for issues with parsed Query
	private List<Issue> getIssuesInQuery(ApplicationUser user) throws SearchException {

		SearchService searchService = ComponentAccessor.getComponent(SearchService.class);

		// String jqlQuery = "project = \"DEMO\" and assignee = currentUser()";
		String jqlQuery = "project = HOSE AND component = CRE ORDER BY created ASC";
		SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlQuery);
		if (parseResult.isValid()) {
			Query query = parseResult.getQuery();

			// IssueSearchParameters params =
			// SearchService.IssueSearchParameters.builder().query(query).build();
			// String queryPath = searchService.getIssueSearchPath(user, params);
			// log.info("Query Path:"+queryPath);
			log.info("Serch query:" + jqlQuery);
			SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
			return getIssuesInSearchResults(results);
		} else {
			log.error("Error parsing query:" + jqlQuery);
			return Collections.emptyList();
		}
	}

	// Build List of BarSeries to analysis.
	public static Collection<BarSeries> buildBarSeries(String projectId, int numberOfBars) {
		
		log.info("Start building Collection of BarSeries: "+numberOfBars+" on "+projectId);
		
		Collection<BarSeries> barCollection = new ArrayList<BarSeries>();
		//Collection<ProjectComponent> stockShortList = getShortListComponent(projectId, numberOfBars);
		Collection<ProjectComponent> stockShortList = getVN30(projectId);
		
		log.info("Finish Collection of BarSeries: "+stockShortList.size()+" for "+projectId);
		Iterator<ProjectComponent> iterator = stockShortList.iterator();
		List<Issue> issues;
		BarSeries series;
		ProjectComponent component;
		
		while(iterator.hasNext()) {
			try {
				component = (ProjectComponent)iterator.next();
				Long componentId = component.getId();
				String symbol = component.getName();
				issues = getIssuesByComponentId(projectId, componentId);
				series = createBarSeriesFromListIssue(issues, symbol);
				log.info("Added a new BarSeries for: "+symbol+", total: "+series.getBarCount()+" bars");
				barCollection.add(series);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return barCollection;
	}
	
	// Return a list of numberOfBars Stock Symbol from projectId 
	public static Collection<String> getShortListStork(String projectId, int numberOfBars) {
		Collection<ProjectComponent> componentList = getAllProjectComponent(projectId);
		Collection<String> stockShortList = new ArrayList<String>();

		// Check numberOfBars is numbers of bars will build
		// numberOfBars == 0 ---> get all bars.
		// numberOfBars > 0 ---> get a list of first numberOfBars series.
		if (numberOfBars == 0 || numberOfBars > componentList.size()) {
			numberOfBars = componentList.size();
		}
		
		Iterator<ProjectComponent> iterator = componentList.iterator();
		int counter = 0;
		ProjectComponent currentComponent = null;
		while (iterator.hasNext()) {
			if (counter < numberOfBars) {
				currentComponent = iterator.next();
				stockShortList.add(currentComponent.getName());				
			}
			++counter;
		}
		return stockShortList;
	}
	
	// Return a list of VN30 Symbol from projectId 
	public static Collection<ProjectComponent> getVN30(String projectId) {
		
		Collection<ProjectComponent> componentList = getAllProjectComponent(projectId);
		Collection<ProjectComponent> stockShortList = new ArrayList<ProjectComponent>();
		final ArrayList<String> vn30 = VN30.getList();

		log.info("Starting VN30 list from project: "+projectId);
		
		// Check only the component in the VN30 list
		
		Iterator<ProjectComponent> iterator = componentList.iterator();
		ProjectComponent current;
		while(iterator.hasNext()) {
			current= iterator.next();
			if(vn30.contains(current.getName())) {
				stockShortList.add(current);
				log.info("VN30 list added: " + current.getName());
			}
		}
		
		log.info("Finish, return a list of VN30: "+stockShortList.size());
		return stockShortList;
	}
	
	// Return a list of numberOfBars Stock Symbol from projectId 
	public static Collection<ProjectComponent> getShortListComponent(String projectId, int numberOfBars) {
		
		Collection<ProjectComponent> componentList = getAllProjectComponent(projectId);
		Collection<ProjectComponent> stockShortList = new ArrayList<ProjectComponent>();

		log.info("Starting component list: "+numberOfBars+" Of "+componentList.size());
		
		// Check numberOfBars is numbers of bars will build
		// numberOfBars == 0 ---> get all bars.
		// numberOfBars > 0 ---> get a list of first numberOfBars series.
		if (numberOfBars == 0 || numberOfBars >= componentList.size()) {
			return componentList;
		}
		
		log.info("Building shorter component list: "+numberOfBars+" Of "+componentList.size());
		Iterator<ProjectComponent> iterator = componentList.iterator();
		for(int i=0; i<numberOfBars; i++) {
			stockShortList.add(iterator.next());;
		}
		
		log.info("Finish, return a list of component: "+stockShortList.size());
		return stockShortList;
	}

	// Return all component of a project using project id (pid)
	public static Collection<ProjectComponent> getAllProjectComponent(String projectId) {

		ProjectComponentManager projectComponentManager = ComponentAccessor.getComponent(ProjectComponentManager.class);
		Project project = ComponentAccessor.getProjectManager().getProjectByCurrentKey(projectId);

		Collection<ProjectComponent> components = null;
		Collection<ProjectComponent> stockComponents = new ArrayList<ProjectComponent>();

		if (project != null) {
			components = projectComponentManager.findAllForProject(project.getId());
			Iterator<ProjectComponent> iterator = components.iterator();
			// Returns an iterator over the elements
			ProjectComponent currentComponent = null;
			while (iterator.hasNext()) {
				currentComponent = iterator.next();
				// Check component name like VIC, ACB which has length is 3.
				if (currentComponent.getName().length() == 3) {
					// Only basic symbol added, other like VNM102, VIC365 will be reject
					stockComponents.add(currentComponent);
				}
			}
		}
		return stockComponents;
	}

	// Search for issues in multiple projects, with empty assignee or reporter
	private List<Issue> getIssuesByComponentObject(String pid, ProjectComponent component) throws SearchException {
		JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

		builder.where().project(pid).and().component().eq(component.getId());
		builder.orderBy().createdDate(SortOrder.ASC);
		Query query = builder.buildQuery();
		SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
		SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

		return getIssuesInSearchResults(results);
	}

	// Search for issues in multiple projects, with empty assignee or reporter
	public static List<Issue> getIssuesByComponentName(String pid, String componentName) throws SearchException {

		Collection<ProjectComponent> componentList = getAllProjectComponent(pid);

		Long componentId = null;
		for (ProjectComponent component : componentList) {
			if (component.getName().equals(componentName)) {
				componentId = component.getId();
			}
		}

		if (null != componentId) {
			JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
			ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

			builder.where().project(pid).and().component().eq(componentId);
			builder.orderBy().createdDate(SortOrder.ASC);
			Query query = builder.buildQuery();
			SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
			SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

			return getIssuesInSearchResults(results);
		}
		return null;

	}
	
	// Search for issues in multiple projects, with empty assignee or reporter
	public static List<Issue> getIssuesByComponentId(String pid, Long componentId) throws SearchException {

		if (null != componentId && null != pid) {
			JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
			ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

			builder.where().project(pid).and().component().eq(componentId);
			builder.orderBy().createdDate(SortOrder.ASC);
			Query query = builder.buildQuery();
			SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
			SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

			return getIssuesInSearchResults(results);
		}
		return null;

	}

	public static List<Issue> getIssuesInSearchResults(SearchResults results) {
		try {
			List<Issue> issues = null;
			// issues = searchResult.getIssues();
			Method newGetMethod = null;

			try {
				newGetMethod = SearchResults.class.getMethod("getIssues");
			} catch (NoSuchMethodException e) {
				try {

					newGetMethod = SearchResults.class.getMethod("getResults");
				} catch (NoSuchMethodError e2) {
					log.error("SearchResults.getResults does not exist!");
				}
			}

			if (newGetMethod != null) {
				issues = (List<Issue>) newGetMethod.invoke(results);
				return issues;
			} else {
			log.error("ERROR NO METHOD TO GET ISSUES !");
				throw new RuntimeException(
						"ICT: SearchResults Service from JIRA NOT AVAILABLE (getIssue / getResults)");
			}

		} catch (Exception e) {
			log.error("Jql Helper can net get search result (ICT)" + e.toString());
		}
		return null;
	}

}