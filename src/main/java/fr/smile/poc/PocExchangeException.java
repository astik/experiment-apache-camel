package fr.smile.poc;

import org.apache.camel.Exchange;

@SuppressWarnings("serial")
public class PocExchangeException extends Exception {
	private Exchange exchange;
	
	public PocExchangeException(Exchange exchange, String message, Throwable cause) {
		super(message, cause);
		this.exchange = exchange;
	}

	public PocExchangeException(Exchange exchange, String message) {
		super(message);
		this.exchange = exchange;
	}

	public Exchange getExchange() {
		return exchange;
	}
}
