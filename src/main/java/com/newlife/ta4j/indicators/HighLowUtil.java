package com.newlife.ta4j.indicators;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.HighestValueIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.LowestValueIndicator;

import com.newlife.indicators.IssuesBarSeries;

public class HighLowUtil {
	
	private static final Logger log = LoggerFactory.getLogger(HighLowUtil.class);
	private static final int barCount = 242;

	public static double build(Collection<IssuesBarSeries> allVN30Symbol, Date created) {
		// Loop through collection to find index base on created date
		if (log.isInfoEnabled()) {
			log.info("Build New High - New Low  for VN30 on: {}",created);
        }
		
		Date dateCurrent;
		double newhigh =0;
		double newlow=0;
		
		for(IssuesBarSeries issuesBarSeries: allVN30Symbol) {
			
			BarSeries series = issuesBarSeries.getSeries();
			
			HighPriceIndicator highPrice = new HighPriceIndicator(series);
			HighestValueIndicator highestYearValue = new HighestValueIndicator(highPrice, barCount);
			
			LowPriceIndicator lowPrice = new LowPriceIndicator(series);
			LowestValueIndicator lowestYearValue = new LowestValueIndicator(lowPrice, barCount);
			
			// Second loog though all bars in a symbol to find climax on givend date
			for(int i =0; i<series.getBarCount(); i++) {
				dateCurrent = Date.from(series.getBar(i).getEndTime().toInstant());
				if(DateUtils.isSameDay(created, dateCurrent)) {
					
					double highest = highestYearValue.getValue(i).doubleValue();
					double high = highPrice.getValue(i).doubleValue();
					if (log.isInfoEnabled()) {
						log.info(i+"=index for created of:"+series.getBar(i).getEndTime()+",high:"+high+", highest:"+highest);
			        }
					if(high == highest) {
						newhigh++;
						log.info("-----------> {} new high founded: {}", newhigh, series.getName());
						break;
					}
					
					double lowest = lowestYearValue.getValue(i).doubleValue();
					double low = highPrice.getValue(i).doubleValue();
					if (log.isInfoEnabled()) {
						log.info(i+"=index for created of:"+series.getBar(i).getEndTime()+",low:"+low+", lowest:"+lowest);
			        }
					if(low == lowest) {
						newlow++;
						log.info("-----------> {} new low founded: {}", newlow, series.getName());
						break;
					}
					
				}
			}
		}
		return newhigh-newlow;
	}

}
