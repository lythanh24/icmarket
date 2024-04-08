package com.newlife.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.num.Num;

public class NetTrendFieldIndicator extends CachedIndicator<Num>  {

	private final OnBalanceVolumeIndicator obvIndicator;
	private final EMAIndicator ema;
	
	
    public NetTrendFieldIndicator(Indicator<Num> indicator) {
    	super(indicator);
        this.obvIndicator = (OnBalanceVolumeIndicator) indicator;
        this.ema = new EMAIndicator(obvIndicator, 9);
    }
    
    @Override
    protected Num calculate(int index) {
    	
    	if (index <= 0) {
            return zero();
        }
    	
    	if(ema.getValue(index).isGreaterThan(ema.getValue(index-1))) {
    		return numOf(1);
    	} else if(ema.getValue(index).isLessThan(ema.getValue(index-1))) {
    		return numOf(-1);
    	}
    	return zero();    	
    }
    
	@Override
	public int getUnstableBars() {
		// TODO Auto-generated method stub
		return 0;
	}

}
