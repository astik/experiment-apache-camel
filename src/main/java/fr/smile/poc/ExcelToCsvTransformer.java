package fr.smile.poc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelToCsvTransformer implements Processor {
	@Override
	public void process(Exchange exchange) throws Exception {
		log.trace("process");
		String inputFileName = (String) exchange.getIn().getHeaders().get(Exchange.FILE_NAME);
		String inputFileNameExtension = FilenameUtils.getExtension(inputFileName);
		Workbook workbook;
		switch (inputFileNameExtension) {
		case "xls":
			workbook = getHSSFWorkbook(exchange);
			break;
		case "xlsx":
			workbook = getXSSFWorkbook(exchange);
			break;
		default:
			throw new IllegalArgumentException(String.format(
					"Unsupported filename extension '%s'. " + "The only supported payload types are xls, xlsx",
					inputFileNameExtension));
		}

		if (workbook == null) {
			throw new PocExchangeException(exchange, "The XlsxToCsv tranformation could'nt be done");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeWorkbookAsCsvToOutputStream(workbook, out);
		} catch (IOException e) {
			throw new PocExchangeException(exchange, "The XlsxToCsv tranformation could'nt be done", e);
		}
		String newFileName = FilenameUtils.getBaseName(inputFileName) + ".csv";

		Message message = exchange.getMessage();
		message.setBody(out.toByteArray());
		message.setHeader(Exchange.FILE_NAME, newFileName);
	}

	private XSSFWorkbook getXSSFWorkbook(Exchange exchange) throws InvalidFormatException, IOException {
		log.trace("getXSSFWorkbook");
		XSSFWorkbook workBook;
		final Object payload = exchange.getIn().getBody();
		if (payload instanceof File) {
			final File filePayload = (File) payload;
			workBook = new XSSFWorkbook(filePayload);
		} else if (payload instanceof byte[]) {
			InputStream inputStream = new ByteArrayInputStream((byte[]) payload);
			workBook = new XSSFWorkbook(inputStream);
		} else {
			throw new IllegalArgumentException(String.format(
					"Unsupported payload type '%s'. "
							+ "The only supported payload types are java.io.File, byte[] and java.io.InputStream",
					payload.getClass().getSimpleName()));
		}
		return workBook;
	}

	private HSSFWorkbook getHSSFWorkbook(Exchange exchange) throws FileNotFoundException, IOException {
		log.trace("getHSSFWorkbook");
		HSSFWorkbook workBook;
		final Object payload = exchange.getIn().getBody();
		if (payload instanceof File) {
			final File filePayload = (File) payload;
			try (InputStream is = new FileInputStream(filePayload)) {
				workBook = new HSSFWorkbook(is);
			}
		} else if (payload instanceof byte[]) {
			InputStream inputStream = new ByteArrayInputStream((byte[]) payload);
			workBook = new HSSFWorkbook(inputStream);
		} else {
			throw new IllegalArgumentException(String.format(
					"Unsupported payload type '%s'. "
							+ "The only supported payload types are java.io.File, byte[] and java.io.InputStream",
					payload.getClass().getSimpleName()));
		}
		return workBook;
	}

	private void writeWorkbookAsCsvToOutputStream(Workbook workbook, OutputStream out) throws IOException {
		log.trace("writeWorkbookAsCsvToOutputStream");
		try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT)) {
			Sheet sheet = workbook.getSheetAt(0); // Sheet #0 in this example
			for (Row row : sheet) {
				if (row != null && row.getLastCellNum() > 0) {
					for (Cell cell : row) {
						switch (cell.getCellType()) {
						case BOOLEAN:
							csvPrinter.print(cell.getBooleanCellValue());
							break;
						case NUMERIC:
							csvPrinter.print(cell.getNumericCellValue());
							break;
						case STRING:
							csvPrinter.print(cell.getStringCellValue());
							break;
						case BLANK:
							csvPrinter.print("");
							break;
						default:
							csvPrinter.print(cell);
						}
					}
					csvPrinter.println();
				}
			}
		}
	}
}
