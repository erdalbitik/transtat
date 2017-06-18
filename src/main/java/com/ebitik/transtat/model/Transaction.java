package com.ebitik.transtat.model;

import java.math.BigDecimal;

/**
 * Java class of Transaction
 * 
 * @author erdal.bitik
 *
 */
public class Transaction {
	
	private BigDecimal amount;
	
	private Long timestamp;

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "Transaction [amount=" + amount + ", timestamp=" + timestamp + "]";
	}

}
