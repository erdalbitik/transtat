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
		return statistic;
	}

	/**
	 * This method maintains Holder objects by adding new transaction
	 * 
	 * @param transaction
	 */
	private void addTransactionToStatistic(Transaction transaction) {
		try {
			lock.lock();
			Long count = statistic.getCount()+1;
			BigDecimal sum = statistic.getSum().add(transaction.getAmount());
			BigDecimal avg = sum.divide(new BigDecimal(count), 2, BigDecimal.ROUND_HALF_UP);
			BigDecimal max = statistic.getMax();
			if(max.compareTo(transaction.getAmount()) < 0) {
				max = transaction.getAmount();
			}
			BigDecimal min = statistic.getMin();
			if(min.compareTo(BigDecimal.ZERO) == 0 || min.compareTo(transaction.getAmount()) > 0) {
				min = transaction.getAmount();
			}
			
			statistic = new Statistic(sum, avg, max, min, count);
			
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
				Long count = statistic.getCount()-1;
				BigDecimal sum = statistic.getSum().subtract(transaction.getAmount());
				BigDecimal avg = BigDecimal.ZERO;
				if(count > 0) {
					avg = sum.divide(new BigDecimal(count), 2, BigDecimal.ROUND_HALF_UP);
				}
				BigDecimal max = statistic.getMax();
				if(max.compareTo(transaction.getAmount()) == 0) {
					max = findMaximumAmount();
				}
				BigDecimal min = statistic.getMin();
				if(min.compareTo(transaction.getAmount()) == 0) {
					min = findMinAmount();
				}

				statistic = new Statistic(sum, avg, max, min, count);

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
