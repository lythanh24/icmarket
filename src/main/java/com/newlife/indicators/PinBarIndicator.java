package com.newlife.indicators;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.num.Num;

public class PinBarIndicator extends CachedIndicator<Boolean>  {

	private final BarSeries series;
	
	
    public PinBarIndicator(BarSeries series) {
    	super(series);
        this.series = series; 
    }
    
    @Override
    protected Boolean calculate(int index) {
    	boolean isWickSatisfy = false;
    	boolean isPrevNextBarSatisfy = false;
    	if (index <= 0) {
            return false;
        }
    	
    	Bar currentBar = series.getBar(index);
    	//System.out.println("--------------- counting bar--------");
    	//System.out.println(currentBar.toString());
    	
    	// Check if the current bar has a small body
        Num bodySize = currentBar.getClosePrice().minus(currentBar.getOpenPrice()).abs();
        Num barSize = currentBar.getHighPrice().minus(currentBar.getLowPrice());
        if (bodySize.isGreaterThan(barSize.multipliedBy(numOf(0.33)))) {
        	//System.out.println(bodySize+ "=bodySize, isGreaterThan barSize*0.33 = "+barSize+"*0.33=" + barSize.multipliedBy(numOf(0.33))+"-->first false");
            return false; // Body is too large, not a pin bar
        }
        
        Num upperWick = currentBar.getHighPrice().minus(currentBar.getClosePrice());
        Num lowerWick = currentBar.getOpenPrice().minus(currentBar.getLowPrice());
        if (upperWick.isLessThan(barSize.multipliedBy(numOf(0.66))) || 
        		lowerWick.isLessThan(barSize.multipliedBy(numOf(0.66)))) {
        	//System.out.println(upperWick+"=upperWick, lowerWick= "+lowerWick+", barSize*0.66="+barSize.multipliedBy(numOf(0.66))+"--> second true");
        	isWickSatisfy = true;
        }

        // Check if the bar is a reversal bar
        if (index > 0 && index < series.getBarCount() - 1) {
        	//System.out.println(".......Check if the bar is a reversal bar..............");
            Bar previousBar = series.getBar(index - 1);
            Bar nextBar = series.getBar(index + 1);
            //System.out.println("previousBar:"+previousBar.toString());
            //System.out.println("nextBar:"+nextBar.toString());
            
            boolean isCurrentPrevClosePriceSatisfy = currentBar.getLowPrice().isLessThan(previousBar.getLowPrice());
            boolean isCurrentNextClosePriceSatisfy = currentBar.getLowPrice().isLessThan(nextBar.getLowPrice());
            //System.out.println("isCurrentPrevClosePriceSatisfy:"+isCurrentPrevClosePriceSatisfy);
            //System.out.println("isCurrentNextClosePriceSatisfy:"+isCurrentNextClosePriceSatisfy);
            
            boolean isCurrentPrevOpenPriceSatisfy = currentBar.getHighPrice().isGreaterThan(previousBar.getHighPrice());
            boolean isCurrentNextOpenPriceSatisfy = currentBar.getHighPrice().isGreaterThan(nextBar.getHighPrice());
            //System.out.println("isCurrentPrevOpenPriceSatisfy:"+isCurrentPrevOpenPriceSatisfy);
            //System.out.println("isCurrentNextOpenPriceSatisfy:"+isCurrentNextOpenPriceSatisfy);
            
            if(isCurrentPrevClosePriceSatisfy && isCurrentNextClosePriceSatisfy) {
            	isPrevNextBarSatisfy = true; 
            } else if(isCurrentPrevOpenPriceSatisfy && isCurrentNextOpenPriceSatisfy) {
            	isPrevNextBarSatisfy = true; 
            }
            
            //System.out.println("------>isPrevNextBarSatisfy:" + isPrevNextBarSatisfy);
            
        } else if(index == series.getBarCount()-1) {
        	//System.out.println(".......Check if the bar is last bar..............");
        	Bar previousBar = series.getBar(index - 1);
        	if ((currentBar.getLowPrice().isLessThan(previousBar.getLowPrice())) 
        			|| (currentBar.getHighPrice().isGreaterThan(previousBar.getHighPrice())) ) {
        		isPrevNextBarSatisfy = true; // Bar is a pin bar
            }
        }
        
        if(isPrevNextBarSatisfy && isWickSatisfy) {
        	//System.out.println("---->Success found pin bar: "+currentBar.toString());
        	return true;
        }
        return false;
    }
    
	@Override
	public int getUnstableBars() {
		// TODO Auto-generated method stub
		return 0;
	}

}
