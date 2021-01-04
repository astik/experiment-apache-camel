package fr.smile.poc;

import static org.apache.camel.language.spel.SpelExpression.spel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.PGPDataFormat;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MyRoute extends RouteBuilder {
	private static final String INPUT_BYTE_ARRAY_CHANNEL = "direct:inputByteArrayChannel";
	private static final String ERROR_CHANNEL = "direct:error";
	private static final String CSV_CHANNEL = "direct:csvChannel";
	private static final String EXCEL_CHANNEL = "direct:excelChannel";
	private static final String ZIP_CHANNEL = "direct:zipChannel";
	private static final String PGP_CHANNEL = "direct:pgpChannel";

	@Value("${poc.input-dir}")
	private String inputDirectory;

	@Value("${poc.output-dir}")
	private String outputDirectory;

	@Value("${poc.pgp.private-key-path}")
	private String pgpPrivateKeyFilePath;

	@Value("${poc.pgp.password}")
	private String pgpPassword;

	@Override
	public void configure() throws Exception {
		errorHandler(deadLetterChannel(ERROR_CHANNEL));

		from("file://" + inputDirectory + "?noop=true")//
				.convertBodyTo(byte[].class) //
				.to(INPUT_BYTE_ARRAY_CHANNEL);

		from(INPUT_BYTE_ARRAY_CHANNEL) //
				.process(new LogHandler("from input", false)) //
				.choice() //
				.when(simple("${file:ext} == 'csv'")).to(CSV_CHANNEL) //
				.when(simple("${file:ext} == 'xls' || ${file:ext} == 'xlsx'")).to(EXCEL_CHANNEL) //
				.when(simple("${file:ext} == 'zip'")).to(ZIP_CHANNEL) //
				.when(simple("${file:ext} endsWith 'gpg' || ${file:ext} endsWith 'pgp'")).to(PGP_CHANNEL) //
				.otherwise().to(ERROR_CHANNEL);

		from(CSV_CHANNEL)//
				.process(new LogHandler("from csv", true)) //
				.to("file://" + outputDirectory);

		from(EXCEL_CHANNEL) //
				.unmarshal(new ExcelFileDataFormat()) //
				.setHeader(Exchange.FILE_NAME, simple("${file:onlyname.noext}.csv")) //
				.to(INPUT_BYTE_ARRAY_CHANNEL);

		from(ZIP_CHANNEL) //
				.split(new ZipSplitter()).streaming() //
				.setHeader(Exchange.FILE_NAME,
						spel("#{T(org.apache.commons.io.FilenameUtils).getName(request.headers['" + Exchange.FILE_NAME
								+ "'])}")) //
				.filter(header(Exchange.FILE_NAME).not().startsWith(".")) //
				.convertBodyTo(byte[].class) //
				.to(INPUT_BYTE_ARRAY_CHANNEL);

		PGPDataFormat pgpDataFormat = new PGPDataFormat();
		pgpDataFormat.setKeyFileName(pgpPrivateKeyFilePath);
		pgpDataFormat.setPassword(pgpPassword);
		from(PGP_CHANNEL)//
				.unmarshal(pgpDataFormat) //
				.setHeader(Exchange.FILE_NAME,
						spel("#{T(org.apache.commons.io.FilenameUtils).getBaseName(request.headers['"
								+ Exchange.FILE_NAME + "'])}")) //
				.to(INPUT_BYTE_ARRAY_CHANNEL);

		from(ERROR_CHANNEL) //
				.process(new LogHandler("from error", false)) //
				.stop();
	}
}
