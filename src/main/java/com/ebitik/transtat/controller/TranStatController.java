package com.ebitik.transtat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ebitik.transtat.model.Statistic;
import com.ebitik.transtat.model.Transaction;
import com.ebitik.transtat.service.TranStatService;

/**
 * transactions and statistics web API
 * 
 * @author erdal.bitik
 *
 */
@RestController
public class TranStatController {

	@Autowired TranStatService service;

	/**
	 * sample json body should be like this
	 * 
	 *  {
	 * 		"amount": 12.3,
	 *		"timestamp": 1478192204000
	 *	}
	 * 
	 * @param transaction
	 * @return ResponseEntity http 201 message with empty body
	 */
	@PostMapping("/transactions")
	public ResponseEntity<Void> addTransaction(@RequestBody Transaction transaction) {
		service.addTransaction(transaction);
		return new ResponseEntity<Void>(new HttpHeaders(), HttpStatus.CREATED); //201
	}
	
	
	/**
	 * spring mvc will return a json like this 
	 * 
	 * {
	 * 	"sum": 1000,
	 * 	"avg": 100,
	 * 	"max": 200,
	 * 	"min": 50,
	 * 	"count": 10
	 * }
	 * 
	 * @return Statistic object
	 */
	@GetMapping("/statistics")
	public @ResponseBody Statistic getStatistic() {
		return service.getStatistic();
	}

}
