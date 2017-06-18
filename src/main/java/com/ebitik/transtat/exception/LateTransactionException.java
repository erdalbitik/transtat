package com.ebitik.transtat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Spring MVC will handle and convert this exception 
 * as an http 204
 * 
 * @author erdal.bitik
 *
 */
@ResponseStatus(value=HttpStatus.NO_CONTENT, reason="Transaction is older than 60 seconds")  // 204
public class LateTransactionException extends RuntimeException {
	private static final long serialVersionUID = 1L;
}
