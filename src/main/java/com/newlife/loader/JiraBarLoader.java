package com.newlife.loader;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jfree.data.general.SeriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.Num;

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
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.order.SortOrder;
import com.newlife.indicators.IssuesBarSeries;
import com.newlife.strategies.TrippleATRStrategy;
import com.newlife.strategies.VolumeStrategy;

public class JiraBarLoader {
	private static final Logger log = LoggerFactory.getLogger(JiraBarLoader.class);

	static JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
	ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
	CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
	IssueService issueService = ComponentAccessor.getIssueService();
	static ProjectComponentManager projectComponentManager = ComponentAccessor
			.getComponent(ProjectComponentManager.class);
	static ProjectManager projectManager = ComponentAccessor.getProjectManager();
	static SearchService searchService = ComponentAccessor.getComponent(SearchService.class);

	public static BarSeries buildWeekBarseries(BarSeries series) {
    	int beginIndex = series.getBeginIndex();
        int endIndex = series.getEndIndex();
        BarSeries weekBarseries = new BaseBarSeries(series.getName());
        
        //build week barseries

        ZonedDateTime date = series.getBar(beginIndex).getBeginTime();
        Num open = series.getBar(beginIndex).getOpenPrice();
        Num high = series.getBar(beginIndex).getHighPrice();
        Num low = series.getBar(beginIndex).getLowPrice();
        Num close = series.getBar(beginIndex).getClosePrice();
        Num volume = series.getBar(beginIndex).getVolume();
        
        //System.out.println("DayOfWeek:"+date.getDayOfWeek()+",name:"+date.getDayOfWeek().name()+",Value:"+date.getDayOfWeek().getValue());
        DayOfWeek dayOfWeek;
        int lastDayOfWeekValue = 0;
        Bar currentBar;
        for(int i =beginIndex; i <= endIndex; i++) {
        	currentBar = series.getBar(i);        	
    		ZonedDateTime currentDate = currentBar.getBeginTime();
            dayOfWeek = currentDate.getDayOfWeek();
            //System.out.println("DayOfWeek:"+dayOfWeek+",name:"+dayOfWeek.name()+",Value:"+dayOfWeek.getValue());
            
            if(lastDayOfWeekValue > dayOfWeek.getValue()) {
            	// The new week come.
            	// Add the previous week bar 
            	//System.out.println("************* addBar and jump to new week,now:"+dayOfWeek.name()+",LastDay:"+lastDayOfWeekValue +",open:"+open+",high:"+high+",close:"+close+",volume:"+volume);
            	weekBarseries.addBar(currentDate, open, high, low, close, volume);
            	lastDayOfWeekValue = 0;
            	
            	// and start a new week bar
            	open = currentBar.getOpenPrice();
                high = currentBar.getHighPrice();
                low = currentBar.getLowPrice();
                close = currentBar.getClosePrice();
                volume = currentBar.getVolume().zero();
            	
            }
            lastDayOfWeekValue = dayOfWeek.getValue();
        	// Calculate the week bar
        	if(currentBar.getHighPrice().isGreaterThan(high)) {
        		high = currentBar.getHighPrice();
        	}
        	if(currentBar.getLowPrice().isLessThan(low)) {
        		low = currentBar.getLowPrice();                		
        	}
        	close = currentBar.getClosePrice();
        	volume = volume.plus(currentBar.getVolume());
        }
        return weekBarseries;

    }
	
	public static Collection<Strategy> findStockByTrippleATRStrategy(String pid, ArrayList<String> symbolList) {
		// Run list of Symbol in HOSE project
		Collection<BarSeries> stockList = buildBarSeries(pid, symbolList);
		Collection<Strategy> strategyList = new ArrayList<Strategy>();

		log.info("Start running for " + stockList.size() + "Symbol");
		Iterator<BarSeries> iterator = stockList.iterator();
		BarSeries series;
		
		while (iterator.hasNext()) {
			try {
				series = iterator.next();

				String symbol = series.getName();

				Strategy strategy = TrippleATRStrategy.buildStrategy(series);

				int endIndex = series.getEndIndex();
				log.info("Checking strategy (" + symbol + ") endIndex " + endIndex + "; " + strategy.getName());

				if (strategy.shouldEnter(endIndex)) {
					strategyList.add(strategy);
					log.info("=========== FOUND A SIGNAL SHOULD ENTER=========");
					log.info("=====" + symbol + " endIndex " + endIndex + "; " + strategy.getName());
					log.info("===================================================");
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return strategyList;
	}
	
	public static Collection<Strategy> findStockByVolumeStrategy(String pid, String volume) {
		// Run list of Symbol in HOSE project
		ArrayList<String> symbolList = new ArrayList<String>(); 
		Collection<BarSeries> stockList = buildBarSeries(pid, symbolList);
		Collection<Strategy> strategyList = new ArrayList<Strategy>();

		log.info("Start running for " + stockList.size() + "Symbol");
		Iterator<BarSeries> iterator = stockList.iterator();
		BarSeries series;
		
		while (iterator.hasNext()) {
			try {
				series = iterator.next();

				String symbol = series.getName();

				Strategy strategy = VolumeStrategy.buildStrategy(series, volume);

				int endIndex = series.getEndIndex();
				log.info("Checking strategy (" + symbol + ") endIndex " + endIndex + "; " + strategy.getName());

				if (strategy.shouldEnter(endIndex)) {
					strategyList.add(strategy);
					log.info("=========== FOUND A SIGNAL SHOULD ENTER=========");
					log.info("=====" + symbol + " endIndex " + endIndex + "; " + strategy.getName());
					log.info("===================================================");
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return strategyList;
	}

	public static Collection<BarSeries> buildBarSeries(String projectId, ArrayList<String> symbolList) {

		log.info("Start building Collection of BarSeries: " + symbolList.size() + "symbol on " + projectId);

		Collection<BarSeries> barCollection = new ArrayList<BarSeries>();
		// Collection<ProjectComponent> stockShortList =
		// getShortListComponent(projectId, numberOfBars);
		Collection<ProjectComponent> stockShortList = getSymbolList(projectId, symbolList);

		log.info("Finish Collection of BarSeries: " + stockShortList.size() + " for " + projectId);
		Iterator<ProjectComponent> iterator = stockShortList.iterator();
		List<Issue> issues;
		BarSeries series;
		ProjectComponent component;

		while (iterator.hasNext()) {
			try {
				component = (ProjectComponent) iterator.next();
				Long componentId = component.getId();
				String symbol = component.getName();
				issues = getIssuesByComponentId(projectId, componentId);
				series = createBarSeriesFromListIssue(issues, symbol);
				log.info("Added a new BarSeries for: " + symbol + ", total: " + series.getBarCount() + " bars");
				barCollection.add(series);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return barCollection;
	}
	
	public static Collection<IssuesBarSeries> buildIssuesBarSeries(String projectId, ArrayList<String> symbolList) {

		log.info("Start building Collection of BarSeries: " + symbolList.size() + "symbol on " + projectId);

		Collection<IssuesBarSeries> barCollection = new ArrayList<IssuesBarSeries>();
		// Collection<ProjectComponent> stockShortList =
		// getShortListComponent(projectId, numberOfBars);
		Collection<ProjectComponent> stockShortList = getSymbolList(projectId, symbolList);

		log.info("Finish Collection of BarSeries: " + stockShortList.size() + " for " + projectId);
		Iterator<ProjectComponent> iterator = stockShortList.iterator();
		List<Issue> issues;
		BarSeries series;
		IssuesBarSeries issuesBarSeries;
		ProjectComponent component;

		while (iterator.hasNext()) {
			try {
				component = (ProjectComponent) iterator.next();
				Long componentId = component.getId();
				String symbol = component.getName();
				issues = getIssuesByComponentId(projectId, componentId);
				series = createBarSeriesFromListIssue(issues, symbol);
				issuesBarSeries = new IssuesBarSeries(issues, series);
				barCollection.add(issuesBarSeries);
				log.info("Added a new BarSeries for: " + symbol + ", total: "+issues.size()+" issues, " + series.getBarCount() + " bars");
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return barCollection;
	}
	
	public static Collection<IssuesBarSeries> buildIssuesBarSeries(String projectId, ArrayList<String> symbolList, Date created) {

		log.info("Start building Collection of BarSeries: " + symbolList.size() + "symbol on " + projectId);

		Collection<IssuesBarSeries> barCollection = new ArrayList<IssuesBarSeries>();
		// Collection<ProjectComponent> stockShortList =
		// getShortListComponent(projectId, numberOfBars);
		Collection<ProjectComponent> stockShortList = getSymbolList(projectId, symbolList);

		log.info("Finish Collection of BarSeries: " + stockShortList.size() + " for " + projectId);
		Iterator<ProjectComponent> iterator = stockShortList.iterator();
		List<Issue> issues;
		BarSeries series;
		IssuesBarSeries issuesBarSeries;
		ProjectComponent component;

		while (iterator.hasNext()) {
			try {
				component = (ProjectComponent) iterator.next();
				Long componentId = component.getId();
				String symbol = component.getName();
				issues = getIssuesByComponentId(projectId, componentId, created);
				series = createBarSeriesFromListIssue(issues, symbol);
				issuesBarSeries = new IssuesBarSeries(issues, series);
				barCollection.add(issuesBarSeries);
				log.info("Added a new BarSeries for: " + symbol + ", total: "+issues.size()+" issues, " + series.getBarCount() + " bars");
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return barCollection;
	}

	// Search for issues in multiple projects, with empty assignee or reporter
	public static List<Issue> getIssuesByComponentId(String pid, Long componentId) throws SearchException {

		if (null != componentId && null != pid) {
			JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
			ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

			builder.where().project(pid).and().component().eq(componentId);
			builder.orderBy().createdDate(SortOrder.ASC);
			Query query = builder.buildQuery();
			
			SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

			return getIssuesInSearchResults(results);
		}
		return null;

	}
	
	// Search for issues in multiple projects, with empty assignee or reporter
		public static List<Issue> getIssuesByComponentId(String pid, Long componentId, Date created) throws SearchException {

			if (null != componentId && null != pid) {
				JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
				ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

				builder.where().project(pid).and().component().eq(componentId).and().createdAfter(created);
				builder.orderBy().createdDate(SortOrder.ASC);
				Query query = builder.buildQuery();
				
				SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());

				return getIssuesInSearchResults(results);
			}
			return null;

		}

	// Return a list of VN30 Symbol from projectId
	public static Collection<ProjectComponent> getSymbolList(String projectId, ArrayList<String> symbolList) {

		Collection<ProjectComponent> componentList = getAllProjectComponent(projectId);
		Collection<ProjectComponent> stockShortList = new ArrayList<ProjectComponent>();

		log.info("Start building symbol list from project: " + projectId);

		// Check only the component in the VN30 list

		Iterator<ProjectComponent> iterator = componentList.iterator();
		ProjectComponent current;
		if(symbolList.size() > 0) {
			// get only a predefined list.
			while (iterator.hasNext()) {
				current = iterator.next();
				if (symbolList.contains(current.getName())) {
					stockShortList.add(current);
					log.info("Symbol list added: " + current.getName());
				}
			}
		} else {
			// Get all list
			while (iterator.hasNext()) {
				current = iterator.next();
				stockShortList.add(current);
				log.info("Symbol list added: " + current.getName());
			}
		}

		log.info("Finish, return a list of VN30: " + stockShortList.size());
		return stockShortList;
	}

	public static BarSeries loadSeries(String pid, String symbol) {
		BarSeries series = null;
		List<Issue> issues;
		try {
			log.info("loadSeries:"+pid+","+symbol);
			issues = getIssuesByComponentName(pid, symbol);

			if (issues == null) {
				return null;
			}

			series = createBarSeriesFromListIssue(issues, symbol);

		} catch (SearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return series;
	}
	public static BarSeries loadVN30Series(String pid, String symbol, Date createdAfter) {
		BarSeries series = null;
		List<Issue> issues;
		try {
			log.info("loadSeries:"+pid+","+symbol);
			issues = getIssuesByComponentName(pid, symbol, createdAfter);

			if (issues == null) {
				return null;
			}

			series = createVN30BarSeries(issues);

		} catch (SearchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return series;
	}

	private static BarSeries createVN30BarSeries(List<Issue> issues) {
		try {
			final BarSeries barSeries = new BaseBarSeries("VN30");

			Issue issue = null;

			for (int i = 0; i < issues.size(); i++) {

				issue = (Issue) issues.get(i);

				// VN30;20240322;1287.63;1295.79;1276.81;1284.14;345960
				String summary = issue.getSummary();
				double climax = Double.parseDouble(issue.getDescription().replace(",", "")); 
				// if climax is over 100 this mean not have climax, so set to 0 to ignore.
				if(climax > 100) {climax =0;}
				String[] parts = summary.trim().split(";");

				ZonedDateTime zonedDateTime = getCreated(summary);
				// Add 15h to set end time.
				zonedDateTime = zonedDateTime.plusHours(15);
				double open = Double.parseDouble(parts[2]);
				double high = Double.parseDouble(parts[3]);
				double low = Double.parseDouble(parts[4]);
				double close = Double.parseDouble(parts[5]);
				double volume = climax;

				log.info("Summary befor: "+summary);
				StringBuffer sb = new StringBuffer();
		        sb.append("Bar OHCL:");sb.append(zonedDateTime);
		        sb.append(", ");sb.append(open);
		        sb.append(", ");sb.append(high);
		        sb.append(", ");sb.append(low);
		        sb.append(", ");sb.append(close);
		        sb.append(", ");sb.append(volume);
				log.info(sb.toString());
				
				try {
					barSeries.addBar(zonedDateTime, open, high, low, close, volume);
				} catch (SeriesException e) {
					log.error(e.toString());
				} catch (IllegalArgumentException ex) {
					log.error(ex.toString());
				}
			}

			return barSeries;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// Search for issues in multiple projects, with empty assignee or reporter
	public static List<Issue> getIssuesByComponentName(String pid, String componentName) throws SearchException {

		Collection<ProjectComponent> componentList = getAllProjectComponent(pid);

		Long componentId = null;
		for (ProjectComponent component : componentList) {
			if (component.getName().equals(componentName)) {
				componentId = component.getId();
				log.info("found symbol:"+componentName+",id:"+componentId);
				break;
			}
		}

		if (null != componentId) {
			log.info("Seach issue for component(symbol)...");
			
			JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
			ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

			builder.where().project(pid).and().component().eq(componentId);
			builder.orderBy().createdDate(SortOrder.ASC);
			Query query = builder.buildQuery();
			SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
			SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
			
			return getIssuesInSearchResults(results);
		}
		log.info("Found Nothing! Return null.");
		return null;

	}
	// Search for issues in multiple projects, with empty assignee or reporter
	public static List<Issue> getIssuesByComponentName(String pid, String componentName, Date createdAfter) throws SearchException {

		Collection<ProjectComponent> componentList = getAllProjectComponent(pid);

		Long componentId = null;
		for (ProjectComponent component : componentList) {
			if (component.getName().equals(componentName)) {
				componentId = component.getId();
				log.info("found symbol:"+componentName+",id:"+componentId);
				break;
			}
		}

		if (null != componentId) {
			log.info("Seach issue for component(symbol) createdAfter:"+createdAfter);
			
			JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
			ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

			builder.where().project(pid).and().component().eq(componentId).and().createdAfter(createdAfter);
			builder.orderBy().createdDate(SortOrder.ASC);
			Query query = builder.buildQuery();
			SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
			SearchResults results = searchService.search(user, query, PagerFilter.getUnlimitedFilter());
			
			return getIssuesInSearchResults(results);
		}
		log.info("Found Nothing! Return null.");
		return null;

	}

	public static BarSeries createBarSeriesFromListIssue(List<Issue> issues, String name) {

		try {
			final BarSeries barSeries = new BaseBarSeries(name);

			Issue issue = null;

			for (int i = 0; i < issues.size(); i++) {

				issue = (Issue) issues.get(i);

				// VN30;20240322;1287.63;1295.79;1276.81;1284.14;345960
				String summary = issue.getSummary();
				String[] parts = summary.trim().split(";");

				ZonedDateTime zonedDateTime = getCreated(summary);
				// Add 15h to set end time.
				zonedDateTime = zonedDateTime.plusHours(15);
				double open = Double.parseDouble(parts[2]);
				double high = Double.parseDouble(parts[3]);
				double low = Double.parseDouble(parts[4]);
				double close = Double.parseDouble(parts[5]);
				double volume = Double.parseDouble(parts[6]);

				log.info("Summary befor: "+summary);
				StringBuffer sb = new StringBuffer();
		        sb.append("Bar OHCL:");sb.append(zonedDateTime);
		        sb.append(", ");sb.append(open);
		        sb.append(", ");sb.append(high);
		        sb.append(", ");sb.append(low);
		        sb.append(", ");sb.append(close);
		        sb.append(", ");sb.append(volume);
				log.info(sb.toString());
				
				try {
					barSeries.addBar(zonedDateTime, open, high, low, close, volume);
				} catch (SeriesException e) {
					log.error(e.toString());
				} catch (IllegalArgumentException ex) {
					log.error(ex.toString());
				}
				
				/**
				if (i == 0) {
					barSeries.addBar(zonedDateTime, open, high, low, close, volume);
				} else {
					Bar lastBar = barSeries.getBar(barSeries.getEndIndex());
					boolean isNewBar = lastBar.getBeginTime().isBefore(zonedDateTime.minusHours(12));

					log.debug("barSeries.getEndIndex()=" + barSeries.getEndIndex());
					log.debug("lastBar.getBeginTime=" + lastBar.getBeginTime() + " VS zonedDateTime: "
							+ zonedDateTime.toString());
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
				}**/
			}

			return barSeries;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static ZonedDateTime getCreated(String summaryStr) {
		String[] parts = summaryStr.trim().split(";");
		String dateStr = parts[1];
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		LocalDateTime time = LocalDate.parse(dateStr, formatter).atTime(9,5);
		
		ZonedDateTime created = time.atZone(ZoneId.systemDefault());
		return created;
	}
	
	public static Date getDateCreated(String summaryStr) {
		String[] parts = summaryStr.trim().split(";");
		String dateStr = parts[1];
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date date;
		try {
			date = formatter.parse(dateStr);

			return date;
			
		} catch (ParseException e) {
			log.error(e.toString());
		}
		return null;
	}

	// Return all component of a project using project id (pid)
	public static Collection<ProjectComponent> getAllProjectComponent(String projectId) {

		Project project = projectManager.getProjectByCurrentKey(projectId);

		Collection<ProjectComponent> components = null;
		Collection<ProjectComponent> stockComponents = new ArrayList<ProjectComponent>();

		if (project != null) {
			components = projectComponentManager.findAllForProject(project.getId());
			Iterator<ProjectComponent> iterator = components.iterator();
			// Returns an iterator over the elements
			ProjectComponent currentComponent = null;
			while (iterator.hasNext()) {
				currentComponent = iterator.next();
				// Check component name like VIC, ACB which has length is 3, and VN100 has lenght is 5.
				if (currentComponent.getName().length() <= 5) {
					// Only basic symbol added, other like VNM102, VIC365 will be reject
					stockComponents.add(currentComponent);
				}
			}
		}
		return stockComponents;
	}

	public static List<Issue> getIssuesInSearchResults(SearchResults results) {
		try {
			List<Issue> issues = null;
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
				log.info("Get a list of issue: "+issues.size());
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
	
	public static void main(String[] args) {
		String date = "VN30;20240402;1,283.44;1,292.3;1,273.16;1292.3;365454";
		ZonedDateTime time = getCreated(date);
		System.out.println("Time: "+time);
	}
}
