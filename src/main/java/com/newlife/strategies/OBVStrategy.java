package com.newlife.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.rules.IsFallingRule;
import org.ta4j.core.rules.IsRisingRule;

public class OBVStrategy {
	
	private static final Logger log = LoggerFactory.getLogger(OBVStrategy.class);

	public static Strategy buildStrategy(BarSeries series, String minVolume) {

		int endIndex = series.getEndIndex();

		OnBalanceVolumeIndicator obv = new OnBalanceVolumeIndicator(series); 

		log.info("OnBalanceVolume [endIndex]" + obv.getValue(endIndex));

		Rule sellingRule = new IsRisingRule(obv, 1);
		Rule buyingRule = new IsFallingRule(obv, 1);

		Strategy strategy = new BaseStrategy(series.getName(), buyingRule, sellingRule);

		return strategy;
	}

}
