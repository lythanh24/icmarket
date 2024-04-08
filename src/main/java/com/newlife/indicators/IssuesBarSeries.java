package com.newlife.indicators;

import java.util.List;

import org.ta4j.core.BarSeries;

import com.atlassian.jira.issue.Issue;

public class IssuesBarSeries {
	
	private List<Issue> issues;
	private BarSeries series;
	
	
	public IssuesBarSeries(List<Issue> issues, BarSeries series) {
		super();
		this.issues = issues;
		this.series = series;
	}
	public List<Issue> getIssues() {
		return issues;
	}
	public void setIssues(List<Issue> issues) {
		this.issues = issues;
	}
	public BarSeries getSeries() {
		return series;
	}
	public void setSeries(BarSeries series) {
		this.series = series;
	}
	

}
