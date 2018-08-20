package com.solace.spring.cloud.stream.binder.util;

public class SolaceMessageConversionException extends RuntimeException {
	public SolaceMessageConversionException(String message) {
		super(message);
	}

	public SolaceMessageConversionException(String message, Throwable throwable) {
		super(message, throwable);
	}
}
