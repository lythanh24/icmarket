package com.newlife.ta4j.servlet;

import static de.sjwimmer.ta4jchart.chartbuilder.IndicatorConfiguration.Builder.of;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.order.SortOrder;
import com.newlife.indicators.IssuesBarSeries;
import com.newlife.loader.JiraBarLoader;
import com.newlife.ta4j.indicators.ClimaxUtil;
import com.newlife.ta4j.indicators.HighLowUtil;

import de.sjwimmer.ta4jchart.chartbuilder.ChartType;
import de.sjwimmer.ta4jchart.chartbuilder.PlotType;
import de.sjwimmer.ta4jchart.chartbuilder.renderer.Theme;


public class VN30ClimaxStrategyServlet extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(VN30ClimaxStrategyServlet.class);

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
		String pid = Optional.ofNullable(req.getParameter("pid")).orElse("HSX");
		String symbol = Optional.ofNullable(req.getParameter("symbol")).orElse("VN30");
		String createdAfterStr = Optional.ofNullable(req.getParameter("created")).orElse("25/12/2022");
		String job = Optional.ofNullable(req.getParameter("job")).orElse("nhnl");

		writer.println("Searching for issues in HSX...<br><br>");

		try {
			
			// Run Vn30 Symbol in HOSE  
		    Date createdAfter = new SimpleDateFormat("dd/MM/yyyy").parse(createdAfterStr);  
			
		    // Building Vn30 series
		    List<Issue> issues = JiraBarLoader.getIssuesByComponentName(pid, symbol, createdAfter);
			series = JiraBarLoader.createBarSeriesFromListIssue(issues,symbol);
			writer.println("VN30 Index: "+issues.size()+" Bars from:"+createdAfter+".<br><br>");
			
			//Building 30 symbols in VN30 Index
			Collection<IssuesBarSeries> allVN30Symbol = JiraBarLoader.buildIssuesBarSeries(pid, HSX.getVN30(), createdAfter);
			writer.println("find list of VN30: "+allVN30Symbol.size()+" symbols.<br><br>");
			
			Iterator<Issue> iterator = issues.iterator();
			Issue issue;
			
			while(iterator.hasNext()) {
				issue = iterator.next();
				
				String summary = issue.getSummary();
				Date created = JiraBarLoader.getDateCreated(summary);
				
				if(job=="climax") {
					double climax = ClimaxUtil.build(allVN30Symbol, created);				
					// Check if climax already exits then update.
					updateClimax(issue, climax);
					
					writer.println("<a href=\"http://localhost:2990/jira/browse/"+issue.getKey()+"\">"+issue.getKey()+"</a>"+issue.getSummary()+" created:"+created+", climax:"+climax+"<br><br>");
					
				}
				if(job =="nhnl") {
					double nhnl = HighLowUtil.build(allVN30Symbol, created);
					updateNewHighNewLow(issue, nhnl);
				}				
				
			}
			writer.println("Done building Climax for VN30:"+issues.size()+" rows updated ----- <br><br>");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writer.println("<br>DONE<br><br>");

		writer.println("Searching for issues in DEMO project, assigned to current user...<br><br>");

		writer.println("<br>DONE<br><br>");

		writer.println("</div></body></html>");
	}
	
	// Update climax into the vn30 issue,
	// Using Description to store climax
	public void updateClimax(Issue currentIssue, double climax) throws IOException {
        log.info(climax+"=climax for iss: "+currentIssue.getSummary());
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setDescription(String.valueOf(climax));

        MutableIssue issue = issueService.getIssue(loggedInUser, currentIssue.getKey()).getIssue();

        IssueService.UpdateValidationResult result =
                issueService.validateUpdate(loggedInUser, issue.getId(), issueInputParameters);

        if (result.getErrorCollection().hasAnyErrors()) {
        	log.error(result.getErrorCollection().getErrors().toString());
        } else {
            issueService.update(loggedInUser, result);
        }
    }
	
	// Update NH-NL into the vn30 issue,
	// Using Environment to store NH-NL
	public void updateNewHighNewLow(Issue currentIssue, double NHNL) throws IOException {
        log.info(NHNL+"=NH-NL for iss: "+currentIssue.getSummary());
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.setEnvironment(String.valueOf(NHNL));

        MutableIssue issue = issueService.getIssue(loggedInUser, currentIssue.getKey()).getIssue();

        IssueService.UpdateValidationResult result =
                issueService.validateUpdate(loggedInUser, issue.getId(), issueInputParameters);

        if (result.getErrorCollection().hasAnyErrors()) {
        	log.error(result.getErrorCollection().getErrors().toString());
        } else {
            issueService.update(loggedInUser, result);
        }
    }

	
}