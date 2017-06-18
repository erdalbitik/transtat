package com.ebitik.transtat.model;

import java.math.BigDecimal;

/**
 * Java class of statistics 
 * 
 * @author erdal.bitik
 *
 */
public class Statistic {
	
	BigDecimal sum;

	BigDecimal avg;

	BigDecimal max;

	BigDecimal min;

	Long count;

	public Statistic(BigDecimal sum, BigDecimal avg, BigDecimal max, BigDecimal min, Long count) {
		super();
		this.sum = sum;
		this.avg = avg;
		this.max = max;
		this.min = min;
		this.count = count;
	}

	@Override
	public String toString() {
		return "Statistic [sum=" + sum + ", avg=" + avg + ", max=" + max + ", min=" + min + ", count=" + count + "]";
	}

	public BigDecimal getSum() {
		return sum;
	}

	public void setSum(BigDecimal sum) {
		this.sum = sum;
	}

	public BigDecimal getAvg() {
		return avg;
	}

	public void setAvg(BigDecimal avg) {
		this.avg = avg;
	}

	public BigDecimal getMax() {
		return max;
	}

	public void setMax(BigDecimal max) {
		this.max = max;
	}

	public BigDecimal getMin() {
		return min;
	}

	public void setMin(BigDecimal min) {
		this.min = min;
	}

	public Long getCount() {
		return count;
	}

	public void setCount(Long count) {
		this.count = count;
	}

}
