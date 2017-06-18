package com.ebitik.transtat;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
	public void test1() throws InterruptedException {
		addNRandomTransaction(5000);
		Statistic statistic = service.getStatistic();
		logger.debug("Statistic: "+statistic);
	}
	
	
	private void addNRandomTransaction(int n) {
		for (int i = 0; i < n; i++) {
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
		}
	}

}
