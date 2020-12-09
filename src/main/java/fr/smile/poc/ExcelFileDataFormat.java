package fr.smile.poc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelFileDataFormat implements DataFormat {
	@Override
	public void start() {
		log.trace("start");
	}

	@Override
	public void stop() {
		log.trace("stop");
	}

	@Override
	public void marshal(Exchange exchange, Object graph, OutputStream stream) throws Exception {
		log.trace("marshal");
		throw new UnsupportedOperationException("Marshal to Excel file is not supported");
	}

	@Override
	public Object unmarshal(Exchange exchange, InputStream stream) throws Exception {
		log.trace("unmarshal");
		String inputFileName = (String) exchange.getIn().getHeaders().get(Exchange.FILE_NAME);
		String inputFileNameExtension = FilenameUtils.getExtension(inputFileName);
		Workbook workbook;
		switch (inputFileNameExtension) {
		case "xls":
			workbook = new HSSFWorkbook(stream);
			break;
		case "xlsx":
			workbook = new XSSFWorkbook(stream);
			break;
		default:
			throw new IllegalArgumentException(String.format(
					"Unsupported filename extension '%s'. " + "The only supported payload types are xls, xlsx",
					inputFileNameExtension));
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeWorkbookAsCsvToOutputStream(workbook, out);
		} catch (IOException e) {
			throw new PocExchangeException(exchange, "The XlsxToCsv tranformation could'nt be done", e);
		}
		return out.toByteArray();
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
