package com.marklogic.entityservices;

import com.marklogic.client.FailedRequestException;

@SuppressWarnings("serial")
public class TestEvalException extends Exception {
	
	public TestEvalException(FailedRequestException e) {
		super(e);
	}

}
