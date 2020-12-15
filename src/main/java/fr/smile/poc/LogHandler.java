package fr.smile.poc;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

/**
 * A very raw logger =D
 */
public class LogHandler implements Processor {
	private String message;
	private boolean displayPayload;

	public LogHandler(String message, boolean displayPayload) {
		super();
		this.message = message;
		this.displayPayload = displayPayload;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		System.out.println("------------> got a message " + message);
		System.out.println("---> headers");
		for (Entry<String, Object> headerEntry : in.getHeaders().entrySet()) {
			System.out.println("     " + headerEntry.getKey() + "=" + headerEntry.getValue());
		}
		if (displayPayload) {
			byte[] payload = (byte[]) in.getBody();
			System.out.println("---> payload " + new String(payload, StandardCharsets.UTF_8));
		}
		Throwable error = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
		if (error != null) {
			error.printStackTrace();
		}
	}
}
