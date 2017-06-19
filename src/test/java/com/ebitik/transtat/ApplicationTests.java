package com.ebitik.transtat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.ebitik.transtat.exception.LateTransactionException;
import com.ebitik.transtat.model.Statistic;
import com.ebitik.transtat.model.Transaction;
import com.ebitik.transtat.service.TranStatService;

/**
 * @author erdal.bitik
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

	private static Logger logger = Logger.getLogger(ApplicationTests.class);

	@Autowired TranStatService service;

	@Test
	public void multiThreadingTest() throws InterruptedException {
		addNRandomTransactionMultiThreaded(10);

		//IntStream.rangeClosed(1, 13).forEach(i -> {
			Statistic statistic = service.getStatistic();
			logger.info("Statistic: "+statistic);
			/*try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}*/
		//});

	}

	private void addNRandomTransactionMultiThreaded(int n) {
		ExecutorService executor = Executors.newFixedThreadPool(10);

		IntStream.range(0, n)
		.forEach(i -> executor.submit(() -> {
			Transaction transaction = new Transaction();
			transaction.setTimestamp(TestUtil.getRandomMinuteTime());
			BigDecimal amount = BigDecimal.valueOf(TestUtil.getRandomAmount());
			amount = amount.setScale(2, RoundingMode.HALF_UP);
			transaction.setAmount(amount);
			try {
				service.addTransaction(transaction);
			} catch (LateTransactionException e) {
				logger.debug("Late Transaction Occured for "+transaction);
			} }
				));

		executor.shutdown();
	}


	private void addNRandomTransaction(int n) {
		IntStream.rangeClosed(1, n).parallel().forEach(i -> {
			Transaction transaction = new Transaction();
			transaction.setTimestamp(TestUtil.getRandomMinuteTime());
			BigDecimal amount = BigDecimal.valueOf(TestUtil.getRandomAmount());
			amount = amount.setScale(2, RoundingMode.HALF_UP);
			transaction.setAmount(amount);
			try {
				service.addTransaction(transaction);
			} catch (LateTransactionException e) {
				logger.debug("Late Transaction Occured for "+transaction);
			}
		});
	}

}
