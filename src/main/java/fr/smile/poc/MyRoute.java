package fr.smile.poc;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MyRoute extends RouteBuilder {
	private static final String INPUT_BYTE_ARRAY_CHANNEL = "seda:inputByteArrayChannel";
	private static final String ERROR_CHANNEL = "seda:error";

	@Value("${poc.input-dir}")
	private String inputDirectory;

	@Value("${poc.output-dir}")
	private String outputDirectory;

	@Override
	public void configure() throws Exception {
		errorHandler(deadLetterChannel(ERROR_CHANNEL));

		from("file://" + inputDirectory + "?noop=true")//
				.convertBodyTo(byte[].class) //
				.to(INPUT_BYTE_ARRAY_CHANNEL);

		from(INPUT_BYTE_ARRAY_CHANNEL) //
				.process(new LogHandler("from input", false)) //
				.choice() //
				// CSV
				.when(simple("${file:ext} == 'csv'")) //
				.process(new LogHandler("from csv", true)) //
				.to("file://" + outputDirectory) //
				// EXCEL
				.when(simple("${file:ext} == 'xls' || ${file:ext} == 'xlsx'")) //
				.unmarshal(new ExcelFileDataFormat()) //
				.setHeader(Exchange.FILE_NAME, simple("${file:onlyname.noext}.csv"))
				.to(INPUT_BYTE_ARRAY_CHANNEL) //
				// ERROR
				.otherwise() //
				.to(ERROR_CHANNEL);

		from(ERROR_CHANNEL) //
				.process(new LogHandler("from error", false));
	}

}