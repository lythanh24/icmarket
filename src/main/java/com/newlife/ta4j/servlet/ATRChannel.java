package com.newlife.ta4j.servlet;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.SeriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.Returns;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.charts.Chart;
import com.atlassian.jira.charts.jfreechart.ChartHelper;
import com.atlassian.jira.charts.jfreechart.PieChartGenerator;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.newlife.loader.JiraBarLoader;

import de.sjwimmer.ta4jchart.chartbuilder.ChartType;
import de.sjwimmer.ta4jchart.chartbuilder.PlotType;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.Theme;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static de.sjwimmer.ta4jchart.chartbuilder.IndicatorConfiguration.Builder.of;

import java.awt.Color;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ATRChannel extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(ATRChannel.class);    

    /** 
    
    **/
    @JiraImport
    private IssueService issueService;
    @JiraImport
    private ProjectService projectService;
    @JiraImport
    private SearchService searchService;
    @JiraImport
    private TemplateRenderer templateRenderer;
    @JiraImport
    private JiraAuthenticationContext authenticationContext;
    @JiraImport
    private ConstantsManager constantsManager;
    
    public ATRChannel(IssueService issueService, ProjectService projectService,
                     SearchService searchService,
                     TemplateRenderer templateRenderer,
                     JiraAuthenticationContext authenticationContext,
                     ConstantsManager constantsManager) {
        this.issueService = issueService;
        this.projectService = projectService;
        this.searchService = searchService;
        this.templateRenderer = templateRenderer;
        this.authenticationContext = authenticationContext;
        this.constantsManager = constantsManager;
    }
    
    /**
    private ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    private CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
	private IssueService issueService = ComponentAccessor.getIssueService();
    private ProjectService projectService = ComponentAccessor.getComponent(ProjectService.class);
    private SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
    private TemplateRenderer templateRenderer = ComponentAccessor.getComponent(TemplateRenderer.class);
    private JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    private ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
    //private static Theme theme = ComponentAccessor.getComponent(Theme.class);
     * 
     */


    private static final String LIST_POSSIBLE_STOCK_TEMPLATE = "/templates/strategy/list.vm";
    private static final String STRATEGY_CHOOSE_TEMPLATE = "/templates/strategy/input.vm";
    private static final String EDIT_TEMPLATE = "/templates/strategy/edit.vm";
    
    private static final String PROJECT_NAME = "HSX";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
    	String action = Optional.ofNullable(req.getParameter("actionType")).orElse("");
    	ApplicationUser user = authenticationContext.getLoggedInUser();
        Map<String, Object> context = new HashMap<>();
        
        context.put("onlineUser", user.getName());
        switch (action) {
            case "searchStock":
            	resp.setContentType("text/html;charset=utf-8");
                templateRenderer.render(STRATEGY_CHOOSE_TEMPLATE, context, resp.getWriter());
                break;
            case "result":
            	log.info("..........Start handle chart......" + action);
                handleTrippleATRStrategy(req, resp);
                break;
            default:
            	resp.setContentType("text/html;charset=utf-8");
                templateRenderer.render(STRATEGY_CHOOSE_TEMPLATE, context, resp.getWriter());
        }
    }    
    
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    	
        String action = Optional.ofNullable(req.getParameter("actionType")).orElse("");
        switch (action) {
            case "runStrategy":
            	handleTrippleATRStrategy(req, resp);
                break;
            default:
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        
    }
	
	private void handleTrippleATRStrategy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// TODO Auto-generated method stub
		String symbol = Optional.ofNullable(req.getParameter("symbol")).orElse("VN50");

		Collection<Strategy> strategyLst = JiraBarLoader.findStockByTrippleATRStrategy(PROJECT_NAME, HSX.getStockList(symbol));
		
		Map<String, Object> context = new HashMap<>();
        
        context.put("strategyList", strategyLst);
        
        templateRenderer.render(LIST_POSSIBLE_STOCK_TEMPLATE, context, resp.getWriter());
	}

	private void handleStrategyWorkForward(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// TODO Auto-generated method stub
		ApplicationUser user = authenticationContext.getLoggedInUser();

        Map<String, Object> context = new HashMap<>();
        
        context.put("VN50", HSX.getStockList(HSX.VN50));

        Project project = projectService.getProjectByKey(user, PROJECT_NAME).getProject();

        context.put("onlineUser", authenticationContext.getLoggedInUser());
        templateRenderer.render(LIST_POSSIBLE_STOCK_TEMPLATE, context, resp.getWriter());
		
	}
	
	/**
     * Retrieve issues using simple JQL query project=PROJECT_NAME
     * Pagination is set to 50
     *
     * @return List of issues
     */
    private List<Issue> getIssues() {

        ApplicationUser user = authenticationContext.getLoggedInUser();
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        Query query = jqlClauseBuilder.project(PROJECT_NAME).buildQuery();
        int expectedIndex = 1;
        int pageSize = 50;
        PagerFilter pagerFilter = PagerFilter.newPageAlignedFilter(expectedIndex,pageSize);

        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(user, query, pagerFilter);
        } catch (SearchException e) {
            e.printStackTrace();
        }
        return searchResults != null ? searchResults.getResults() : null;
    }
    
    public Chart generateChart(JiraAuthenticationContext authenticationContext, int width, int height) {
        try {
          final Map<String, Object> params = new HashMap<String, Object>();
          // Create Dataset
          DefaultPieDataset dataset = new DefaultPieDataset();

          dataset.setValue("One", 10L);
          dataset.setValue("Two", 15L);

          final ChartHelper helper = new PieChartGenerator(dataset, authenticationContext.getI18nHelper()).generateChart();
          helper.generate(width, height);

          params.put("chart", helper.getLocation());
          params.put("chartDataset", dataset);
          params.put("imagemap", helper.getImageMap());
          params.put("imagemapName", helper.getImageMapName());
          params.put("width", width);
          params.put("height", height);

          return new Chart(helper.getLocation(), helper.getImageMap(), helper.getImageMapName(), params);

        } catch (Exception e) {
          e.printStackTrace();
          throw new RuntimeException("Error generating chart", e);
        }
    }
	
	public static synchronized JFreeChart buildTACChart(BarSeries barSeries) {

		final VolumeIndicator volume = new VolumeIndicator(barSeries);
		final ParabolicSarIndicator parabolicSar = new ParabolicSarIndicator(barSeries);
		final ClosePriceIndicator closePrice = new ClosePriceIndicator(barSeries);
		final EMAIndicator longEma = new EMAIndicator(closePrice, 12);
		final EMAIndicator shortEma = new EMAIndicator(closePrice, 4);
		final CrossedDownIndicatorRule exit = new CrossedDownIndicatorRule(shortEma, longEma);
		final CrossedUpIndicatorRule entry = new CrossedUpIndicatorRule(shortEma, longEma);
		
		final Strategy strategy = new BaseStrategy(entry, exit);
		
		log.info("Building Chart from BarSeries:" + barSeries.getBarCount() + ", Long ema:"  + longEma.getValue(0));
		log.info("Strategy: "+strategy.shouldEnter(barSeries.getEndIndex()));

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
				.withIndicator(of(longEma).name("Long Ema").plotType(PlotType.SUBPLOT).chartType(ChartType.LINE)).getChart(); // random
					
		return chart;

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

			return barSeries;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}