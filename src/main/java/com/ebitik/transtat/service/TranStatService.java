package com.ebitik.transtat.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
	 * the total sum of transaction value in the last 60 seconds
	 * */
	AtomicReference<BigDecimal> sumHolder = new AtomicReference<>(BigDecimal.ZERO);
	/**
	 * the average amount of transaction value in the last 60 seconds
	 * */
	AtomicReference<BigDecimal> avgHolder = new AtomicReference<>(BigDecimal.ZERO);
	/**
	 * single highest transaction value in the last 60 seconds
	 * */
	AtomicReference<BigDecimal> maxHolder = new AtomicReference<>(BigDecimal.ZERO);
	/**
	 * single lowest transaction value in the last 60 seconds
	 * */
	AtomicReference<BigDecimal> minHolder = new AtomicReference<>(BigDecimal.ZERO);
	/**
	 * the total number of transactions happened in the last 60 seconds
	 * */
	AtomicReference<Long> countHolder = new AtomicReference<>(new Long(0));

	
	/**
	 * This method adds a transaction for statistical purpose
	 * 
	 * @param transaction
	 */
	public synchronized void addTransaction(Transaction transaction) {
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
	 * This method returns current Statistic data which is ready.
	 * 
	 * @return Statistic
	 */
	public Statistic getStatistic() {
		return new Statistic(sumHolder.get(), avgHolder.get(), maxHolder.get(), minHolder.get(), countHolder.get());
	}

	/**
	 * This method maintains Holder objects by adding new transaction
	 * 
	 * @param transaction
	 */
	private void addTransactionToStatistic(Transaction transaction) {
		countHolder.updateAndGet(x -> x+1);
		sumHolder.updateAndGet(x -> x.add(transaction.getAmount()));
		avgHolder.updateAndGet(x -> sumHolder.get().divide(new BigDecimal(countHolder.get()), 2, BigDecimal.ROUND_HALF_UP));
		if(maxHolder.get().compareTo(transaction.getAmount()) < 0) {
			maxHolder.updateAndGet(x -> transaction.getAmount());
		}
		if(minHolder.get().compareTo(BigDecimal.ZERO) == 0 || minHolder.get().compareTo(transaction.getAmount()) > 0) {
			minHolder.updateAndGet(x -> transaction.getAmount());
		}
	}

	/**
	 * This method maintains Holder objects by removing new transaction
	 * 
	 * this method is called by expiring map automatically, when a transaction expires.
	 * 
	 * @param transaction
	 */
	private synchronized void removeTransactionFromStatistic(List<Transaction> transactionList) {
		for (Transaction transaction : transactionList) {
			logger.debug("Expiring : " +transaction);
			Long count = countHolder.updateAndGet(x -> x-1);
			BigDecimal sum = sumHolder.updateAndGet(x -> x.subtract(transaction.getAmount()));
			BigDecimal avg = BigDecimal.ZERO;
			if(count > 0) {
				avg = sum.divide(new BigDecimal(count), 2, BigDecimal.ROUND_HALF_UP);
			}
			avgHolder.set(avg);
			if(maxHolder.get().compareTo(transaction.getAmount()) == 0) {
				maxHolder.set(findMaximumAmount());
			} else if(minHolder.get().compareTo(transaction.getAmount()) == 0) {
				minHolder.set(findMinAmount());
			}
			logger.debug("Expired: "+transaction);
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
