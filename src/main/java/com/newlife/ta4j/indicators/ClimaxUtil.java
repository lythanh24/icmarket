package com.newlife.ta4j.indicators;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;

import com.atlassian.jira.issue.Issue;
import com.newlife.indicators.IssuesBarSeries;
import com.newlife.indicators.NetTrendFieldIndicator;
import com.newlife.loader.JiraBarLoader;

public class ClimaxUtil {
	
	private static final Logger log = LoggerFactory.getLogger(ClimaxUtil.class);

	public static double build(Collection<IssuesBarSeries> allVN30Symbol, Date created) {
		// Loop through collection to find index base on created date
		log.info("Build climax for VN30 on "+created);
		double climax = 0;
		Date dateCreated = Date.from(created.toInstant());
		Date dateCurrent;
		
		// First loop though 30 symbols in VN30.
		for(IssuesBarSeries issuesBarSeries: allVN30Symbol) {
			
			BarSeries series = issuesBarSeries.getSeries();
			
			// Second loog though all bars in a symbol to find climax on givend date
			for(int i =0; i<series.getBarCount(); i++) {
				dateCurrent = Date.from(series.getBar(i).getEndTime().toInstant());
				if(DateUtils.isSameDay(dateCreated, dateCurrent)) {
					climax = climax + getNetTrendField(series, i);
					log.info(i+"=index for created of:"+series.getBar(i).getEndTime()+":---"+created+", climax:"+climax);
					break;
				}
			}
		}
		log.info(created+" all VN30 have climax: "+climax);
		return climax;
	}
	
	private static int getNetTrendField(BarSeries barSeries, int index) {
		
		OnBalanceVolumeIndicator obvIndicator = new OnBalanceVolumeIndicator(barSeries);
		NetTrendFieldIndicator ntf = new NetTrendFieldIndicator(obvIndicator);
		
		return ntf.getValue(index).intValue();
		
	}
	
	public static void main(String[] args) {
		
	    try {
	    	String sDate1="31/12/2022";  
			Date created1 = new SimpleDateFormat("dd/MM/yyyy").parse(sDate1);
			
			String sDate2="30/12/2022";  
			Date created2 = new SimpleDateFormat("dd/MM/yyyy").parse(sDate2);
			
			int compare = created1.compareTo(created2);
			System.out.println("compare:"+compare);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
	}

}
