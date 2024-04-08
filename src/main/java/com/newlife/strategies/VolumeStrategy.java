package com.newlife.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.rules.IsRisingRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

public class VolumeStrategy {
	private static final Logger log = LoggerFactory.getLogger(VolumeStrategy.class);

	public static Strategy buildStrategy(BarSeries series, String minVolume) {

		int endIndex = series.getEndIndex();
		double minVolumeValue = Double.valueOf(minVolume);

		ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
		VolumeIndicator volume = new VolumeIndicator(series);
		OnBalanceVolumeIndicator obv = new OnBalanceVolumeIndicator(series); 

		log.info("Close prices [endIndex]" + closePrices.getValue(endIndex));

		EMAIndicator volumeEma = new EMAIndicator(volume, Constant.MACD_SHORT_TIMEFRAME);

		log.info("Volume [endIndex]" + volume.getValue(endIndex) + ", plusMacd: " + volumeEma.getValue(endIndex));

		
		Rule sellingRule = new OverIndicatorRule(volume, volumeEma)
				.and(new OverIndicatorRule(volumeEma, minVolumeValue));
		Rule buyingRule = new UnderIndicatorRule(volume, volumeEma);

		Strategy strategy = new BaseStrategy(series.getName(), buyingRule, sellingRule);

		return strategy;
	}

}
