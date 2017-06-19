package com.ebitik.transtat.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.ebitik.transtat.exception.LateTransactionException;
import com.ebitik.transtat.model.Statistic;
import com.ebitik.transtat.model.Transaction;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * @author erdal.bitik
 *
 */
@Service
public class TranStatService {

	private static Logger logger = Logger.getLogger(TranStatService.class);

	/**
	 * Keeps all transactions in 60 seconds, will be located in this map.
	 * This map will automatically expires an entry which is older than 60 seconds.
	 * key of map is timestamp and value of map is a transaction list. 
	 * (There can be multiple transactions with same timestamp)
	 * */
	ExpiringMap<Long, List<Transaction>> expiringMap = ExpiringMap.builder()
			.expirationPolicy(ExpirationPolicy.CREATED)
			.variableExpiration()
			.asyncExpirationListener((key, transactionList) -> removeTransactionFromStatistic((List<Transaction>) transactionList))
			.build();
	
	/**
	 * single statistic object. addTransactionToStatistic and 
	 * removeTransactionFromStatistic methods maintain this object
	 * */
	private Statistic statistic = new Statistic(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Long.valueOf(0));
	
	/**
	 * lock object
	 * */
	ReentrantLock lock = new ReentrantLock();

	/**
	 * This method adds a transaction for statistical purpose
	 * 
	 * @param transaction
	 */
	public void addTransaction(Transaction transaction) {
		logger.debug("Adding: "+transaction);

		Long transactionTimestamp = transaction.getTimestamp();
		//Find UTC millisecond value of 60 seconds before.
		Long omaTimestamp = Instant.now().toEpochMilli()-60000;

		//throw an exception if transaction is out of last 60 seconds
		if(transactionTimestamp < omaTimestamp) {
			throw new LateTransactionException();
		}

		//calculate sum, avg, count, min and max for statistics
		addTransactionToStatistic(transaction);

		//find duration of this transaction. This transaction will be alive in this duration
		long duration = (transactionTimestamp-omaTimestamp);

		//put transaction to expiringMap with its duration
		List<Transaction> transactionList = expiringMap.get(transactionTimestamp);
		if(Objects.isNull(transactionList)) {
			transactionList = new ArrayList<>();
		}
		transactionList.add(transaction);
		expiringMap.put(transactionTimestamp, transactionList, ExpirationPolicy.CREATED, duration, TimeUnit.MILLISECONDS);

		logger.debug("Added: "+transaction);
	}

	/**
	 * This method returns current Statistic data which is always ready.
	 * 
	 * @return Statistic
	 */
	public Statistic getStatistic() {
		try {
			lock.lock();
			return statistic;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * This method maintains Holder objects by adding new transaction
	 * 
	 * @param transaction
	 */
	private void addTransactionToStatistic(Transaction transaction) {
		try {
			lock.lock();
			statistic.setCount(statistic.getCount()+1);
			statistic.setSum(statistic.getSum().add(transaction.getAmount()));
			statistic.setAvg(statistic.getSum().divide(new BigDecimal(statistic.getCount()), 2, BigDecimal.ROUND_HALF_UP));
			if(statistic.getMax().compareTo(transaction.getAmount()) < 0) {
				statistic.setMax(transaction.getAmount());
			}
			if(statistic.getMin().compareTo(BigDecimal.ZERO) == 0 || statistic.getMin().compareTo(transaction.getAmount()) > 0) {
				statistic.setMin(transaction.getAmount());
			}
		} finally {
			lock.unlock();
		}

	}

	/**
	 * This method maintains Holder objects by removing new transaction
	 * 
	 * this method is called by expiring map automatically, when a transaction expires.
	 * 
	 * @param transaction
	 */
	private void removeTransactionFromStatistic(List<Transaction> transactionList) {
		for (Transaction transaction : transactionList) {
			try {
				lock.lock();
				logger.debug("Expiring : " +transaction);
				statistic.setCount(statistic.getCount()-1);
				statistic.setSum(statistic.getSum().subtract(transaction.getAmount()));
				statistic.setAvg(BigDecimal.ZERO);
				if(statistic.getCount() > 0) {
					statistic.setAvg(statistic.getSum().divide(new BigDecimal(statistic.getCount()), 2, BigDecimal.ROUND_HALF_UP));
				}
				if(statistic.getMax().compareTo(transaction.getAmount()) == 0) {
					statistic.setMax(findMaximumAmount());
				}
				if(statistic.getMin().compareTo(transaction.getAmount()) == 0) {
					statistic.setMin(findMinAmount());
				}
				logger.debug("Expired: "+transaction);
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Traverses all map and finds maximum transaction value
	 * 
	 * @return BigDecimal
	 */
	private BigDecimal findMaximumAmount() {
		Optional<BigDecimal> maxOpt = expiringMap.entrySet().stream().flatMap(m -> m.getValue().stream()).map(m -> m.getAmount()).max((a1, a2) -> a1.compareTo(a2));
		if(maxOpt.isPresent()) {
			return maxOpt.get();
		} else {
			return BigDecimal.ZERO;
		}
	}

	/**
	 * Traverses all map and finds minimum transaction value
	 * 
	 * @return BigDecimal
	 */
	private BigDecimal findMinAmount() {
		Optional<BigDecimal> minOpt = expiringMap.entrySet().stream().flatMap(m -> m.getValue().stream()).map(m -> m.getAmount()).min((a1, a2) -> a1.compareTo(a2));
		if(minOpt.isPresent()) {
			return minOpt.get();
		} else {
			return BigDecimal.ZERO;
		}
	}

}
