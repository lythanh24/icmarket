package com.newlife.strategies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.rules.BooleanRule;
import org.ta4j.core.rules.IsFallingRule;
import org.ta4j.core.rules.IsRisingRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;


public class TrippleATRStrategy {
	private static final Logger log = LoggerFactory.getLogger(TrippleATRStrategy.class);

	public static Strategy buildStrategy(BarSeries series) {
		
		int endIndex = series.getEndIndex();
		ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
		LowPriceIndicator lowPrices = new LowPriceIndicator(series);
		HighPriceIndicator highPrices = new HighPriceIndicator(series);
		VolumeIndicator volume = new VolumeIndicator(series);
		
        log.info("Close prices [endIndex]"+ closePrices.getValue(endIndex));
        
        EMAIndicator longEma = new EMAIndicator(closePrices, Constant.EMA_LONG_TIMEFRAME);
        EMAIndicator volumeEma = new EMAIndicator(volume, Constant.EMA_SHORT_TIMEFRAME);
        
        // Getting the first ATRIndicator
        ATRIndicator aTR = new ATRIndicator(series, 14);
        
        // Getting 2ATR
        TransformIndicator doubleATR = TransformIndicator.multiply(aTR, 2.0);
        
        // Getting 2ATR
        TransformIndicator trippleATR = TransformIndicator.multiply(aTR, 3.0); 
        
        // Getting ATR Channel
        CombineIndicator aTRBand = CombineIndicator.plus(aTR, longEma); 
        CombineIndicator doubleATRBand = CombineIndicator.plus(doubleATR, longEma);
        CombineIndicator trippleATRBand = CombineIndicator.plus(trippleATR, longEma);

        // Getting ATR Channel
        CombineIndicator aTRDownBand = CombineIndicator.minus(longEma, aTR); 
        CombineIndicator doubleDownATRBand = CombineIndicator.minus(longEma, doubleATR);
        CombineIndicator trippleDownATRBand = CombineIndicator.minus(longEma, trippleATR);
        
        MACDIndicator macd = new MACDIndicator(closePrices, Constant.EMA_SHORT_TIMEFRAME, Constant.EMA_LONG_TIMEFRAME);
        TransformIndicator plusMacd = TransformIndicator.multiply(macd, 1000.0);
        EMAIndicator emaMacd = new EMAIndicator(plusMacd, Constant.MACD_SHORT_TIMEFRAME);        
        CombineIndicator histogram = CombineIndicator.minus(plusMacd, emaMacd);        
        
        log.info("MACD [endIndex]"+ macd.getValue(endIndex).toString() + ", plusMacd: " + plusMacd.getValue(endIndex) 
        	+ " emaMacd: "+emaMacd.getValue(endIndex).toString() 
        	+ " histogram: "+histogram.getValue(endIndex).toString());
        
        //Buy on down trend
        Rule isFallingRule = new IsFallingRule(longEma, 3);
        Rule buyingOnDownTrendRule = isFallingRule.and((new UnderIndicatorRule(lowPrices, doubleDownATRBand)));
        
        //Buy on up trend
        Rule isRisingRule = new IsRisingRule(longEma, 3);
        Rule buyingOnUpTrendRule = isRisingRule.and((new UnderIndicatorRule(lowPrices, aTRBand)));
        
        Rule buyingRule = buyingOnDownTrendRule.or(buyingOnUpTrendRule);

        log.info("Check Buying Rule: [histogramIsRising]:"+isRisingRule.isSatisfied(endIndex));
        log.info("Check Buying Rule: [lowPrices]:"+lowPrices.getValue(endIndex)+", [aTRDownBand]:"
        +aTRDownBand.getValue(endIndex)+",[trippleDownATRBand]"+trippleDownATRBand.getValue(endIndex));
        
        Rule histogramIsFailing = new IsFallingRule(histogram, 2);
        Rule overATRBand = new OverIndicatorRule(highPrices, aTRBand);
        Rule sellingRule = histogramIsFailing.and(overATRBand
        		.or(new OverIndicatorRule(highPrices, doubleATRBand))
        		.or(new OverIndicatorRule(highPrices, trippleATRBand)));

        Strategy strategy = new BaseStrategy(series.getName(),buyingRule, sellingRule);
        return strategy;
	}

}
