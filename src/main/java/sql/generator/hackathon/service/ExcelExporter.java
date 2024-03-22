package sql.generator.hackathon.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

import sql.generator.hackathon.model.ColumnInfo;

@Service
public class ExcelExporter {
	private Map<String, List<List<ColumnInfo>>> dataList;

	private HSSFCellStyle createHeaderRow(HSSFWorkbook workbook) {
		HSSFFont font = workbook.createFont();
		font.setBold(true);
		HSSFCellStyle style = workbook.createCellStyle();
		style.setFont(font);
		return style;

	}

	public HSSFWorkbook createEex(Map<String, List<List<ColumnInfo>>> dataList, List<String> listMarkColors) {
		HSSFWorkbook workbook = new HSSFWorkbook();
		dataList.entrySet().forEach(entry -> {
			System.out.println("Name Table" + entry.getKey());
			HSSFSheet sheet = workbook.createSheet(entry.getKey());
			List<List<ColumnInfo>> isNameTable = entry.getValue();

			System.out.println("reocrd" + isNameTable.size());
			int rownum = 0;
			for (List<ColumnInfo> imtem : isNameTable) {

				Row row = sheet.createRow(rownum);
				// Auto size all the columns
				if (rownum == 0) {
					createheader(sheet, imtem, row, workbook);
					rownum++;
					row = sheet.createRow(rownum);
					createRow(sheet, imtem, row, workbook, listMarkColors);
				} else {
					createRow(sheet, imtem, row, workbook, listMarkColors);

				}
				rownum++;
			}
		});

		return workbook;
	}

	private void createRow(Sheet sheet, List<ColumnInfo> isNameTable, Row row, HSSFWorkbook workbook,
			List<String> listMarkColors) {
		Map<String, Short> listMapingMarkColor = mapingColer(listMarkColors);
		for (int i = 0; i < isNameTable.size(); i++) {

			Cell cell = row.createCell(i, CellType.STRING);
			cell.setCellValue(isNameTable.get(i).val);
			for (String entry : listMapingMarkColor.keySet()) {
				if (isNameTable.get(i).color != null) {
					if (isNameTable.get(i).color.equals(entry)) {
						CellStyle style1 = workbook.createCellStyle();
						style1.setFillForegroundColor(listMapingMarkColor.get(entry));
						Font font = workbook.createFont();
						font.setColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
						style1.setFont(font);
						style1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
						cell.setCellStyle(style1);
					}
				} else {
					CellStyle style1_a2 = workbook.createCellStyle();
					cell.setCellStyle(style1_a2);
					cell.setCellStyle(style1_a2);
				}
			}
		}
	}

	private Map<String, Short> mapingColer(List<String> listMarkColors) {
		Map<String, Short> listMapingMarkColor = new HashMap<>();

		listMarkColors.forEach((temp) -> {
			// variable: Một biến để kiểm tra.
			switch (temp) {
			case "MARK_COLOR_1":
				listMapingMarkColor.put("MARK_COLOR_1", HSSFColor.HSSFColorPredefined.DARK_RED.getIndex());
				break;
			case "MARK_COLOR_2":
				listMapingMarkColor.put("MARK_COLOR_2", HSSFColor.HSSFColorPredefined.BROWN.getIndex());
				break;
			case "MARK_COLOR_3":
				listMapingMarkColor.put("MARK_COLOR_3", HSSFColor.HSSFColorPredefined.OLIVE_GREEN.getIndex());
			case "MARK_COLOR_4":
				listMapingMarkColor.put("MARK_COLOR_4", HSSFColor.HSSFColorPredefined.DARK_GREEN.getIndex());
			case "MARK_COLOR_5":
				listMapingMarkColor.put("MARK_COLOR_5", HSSFColor.HSSFColorPredefined.DARK_TEAL.getIndex());
			case "MARK_COLOR_6":
				listMapingMarkColor.put("MARK_COLOR_6", HSSFColor.HSSFColorPredefined.DARK_BLUE.getIndex());
			case "MARK_COLOR_7":
				listMapingMarkColor.put("MARK_COLOR_7", HSSFColor.HSSFColorPredefined.INDIGO.getIndex());
			case "MARK_COLOR_8":
				listMapingMarkColor.put("MARK_COLOR_8", HSSFColor.HSSFColorPredefined.GREY_80_PERCENT.getIndex());
			case "MARK_COLOR_9":
				listMapingMarkColor.put("MARK_COLOR_9", HSSFColor.HSSFColorPredefined.ORANGE.getIndex());
			case "MARK_COLOR_10":
				listMapingMarkColor.put("MARK_COLOR_10", HSSFColor.HSSFColorPredefined.DARK_YELLOW.getIndex());
			case "MARK_COLOR_11":
				listMapingMarkColor.put("MARK_COLOR_11", HSSFColor.HSSFColorPredefined.GREEN.getIndex());
			case "MARK_COLOR_12":
				listMapingMarkColor.put("MARK_COLOR_12", HSSFColor.HSSFColorPredefined.TEAL.getIndex());
			case "MARK_COLOR_13":
				listMapingMarkColor.put("MARK_COLOR_13", HSSFColor.HSSFColorPredefined.BLUE.getIndex());
			case "MARK_COLOR_14":
				listMapingMarkColor.put("MARK_COLOR_14", HSSFColor.HSSFColorPredefined.BLUE_GREY.getIndex());
			case "MARK_COLOR_15":
				listMapingMarkColor.put("MARK_COLOR_15", HSSFColor.HSSFColorPredefined.GREY_50_PERCENT.getIndex());
			case "MARK_COLOR_16":
				listMapingMarkColor.put("MARK_COLOR_16", HSSFColor.HSSFColorPredefined.RED.getIndex());
			case "MARK_COLOR_17":
				listMapingMarkColor.put("MARK_COLOR_17", HSSFColor.HSSFColorPredefined.LIGHT_ORANGE.getIndex());
			case "MARK_COLOR_18":
				listMapingMarkColor.put("MARK_COLOR_18", HSSFColor.HSSFColorPredefined.LIME.getIndex());
			case "MARK_COLOR_19":
				listMapingMarkColor.put("MARK_COLOR_19", HSSFColor.HSSFColorPredefined.SEA_GREEN.getIndex());
			case "MARK_COLOR_20":
				listMapingMarkColor.put("MARK_COLOR_20", HSSFColor.HSSFColorPredefined.AQUA.getIndex());
			case "MARK_COLOR_21":
				listMapingMarkColor.put("MARK_COLOR_21", HSSFColor.HSSFColorPredefined.LIGHT_BLUE.getIndex());
			case "MARK_COLOR_22":
				listMapingMarkColor.put("MARK_COLOR_22", HSSFColor.HSSFColorPredefined.VIOLET.getIndex());
			case "MARK_COLOR_23":
				listMapingMarkColor.put("MARK_COLOR_23", HSSFColor.HSSFColorPredefined.GREY_40_PERCENT.getIndex());
			case "MARK_COLOR_24":
				listMapingMarkColor.put("MARK_COLOR_24", HSSFColor.HSSFColorPredefined.PINK.getIndex());
			case "MARK_COLOR_25":
				listMapingMarkColor.put("MARK_COLOR_25", HSSFColor.HSSFColorPredefined.GOLD.getIndex());
			case "MARK_COLOR_26":
				listMapingMarkColor.put("MARK_COLOR_26", HSSFColor.HSSFColorPredefined.YELLOW.getIndex());
			case "MARK_COLOR_27":
				listMapingMarkColor.put("MARK_COLOR_27", HSSFColor.HSSFColorPredefined.BRIGHT_GREEN.getIndex());
			case "MARK_COLOR_28":
				listMapingMarkColor.put("MARK_COLOR_28", HSSFColor.HSSFColorPredefined.TURQUOISE.getIndex());
			case "MARK_COLOR_29":
				listMapingMarkColor.put("MARK_COLOR_29", HSSFColor.HSSFColorPredefined.BLACK.getIndex());
			case "MARK_COLOR_30":
				listMapingMarkColor.put("MARK_COLOR_30", HSSFColor.HSSFColorPredefined.SKY_BLUE.getIndex());
			case "MARK_COLOR_31":
				listMapingMarkColor.put("MARK_COLOR_31", HSSFColor.HSSFColorPredefined.PLUM.getIndex());
			case "MARK_COLOR_32":
				listMapingMarkColor.put("MARK_COLOR_32", HSSFColor.HSSFColorPredefined.GREY_25_PERCENT.getIndex());
			case "MARK_COLOR_33":
				listMapingMarkColor.put("MARK_COLOR_33", HSSFColor.HSSFColorPredefined.ROSE.getIndex());
			case "MARK_COLOR_34":
				listMapingMarkColor.put("MARK_COLOR_34", HSSFColor.HSSFColorPredefined.LIGHT_YELLOW.getIndex());
			case "MARK_COLOR_35":
				listMapingMarkColor.put("MARK_COLOR_35", HSSFColor.HSSFColorPredefined.LIGHT_GREEN.getIndex());
			case "MARK_COLOR_36":
				listMapingMarkColor.put("MARK_COLOR_36", HSSFColor.HSSFColorPredefined.LIGHT_TURQUOISE.getIndex());
			case "MARK_COLOR_37":
				listMapingMarkColor.put("MARK_COLOR_37", HSSFColor.HSSFColorPredefined.PALE_BLUE.getIndex());
			case "MARK_COLOR_38":
				listMapingMarkColor.put("MARK_COLOR_38", HSSFColor.HSSFColorPredefined.LAVENDER.getIndex());
			case "MARK_COLOR_39":
				listMapingMarkColor.put("MARK_COLOR_39", HSSFColor.HSSFColorPredefined.WHITE.getIndex());
			case "MARK_COLOR_40":
				listMapingMarkColor.put("MARK_COLOR_40", HSSFColor.HSSFColorPredefined.CORNFLOWER_BLUE.getIndex());
			case "MARK_COLOR_41":
				listMapingMarkColor.put("MARK_COLOR_41", HSSFColor.HSSFColorPredefined.LEMON_CHIFFON.getIndex());
			case "MARK_COLOR_42":
				listMapingMarkColor.put("MARK_COLOR_42", HSSFColor.HSSFColorPredefined.MAROON.getIndex());
			case "MARK_COLOR_43":
				listMapingMarkColor.put("MARK_COLOR_43", HSSFColor.HSSFColorPredefined.ORCHID.getIndex());
			case "MARK_COLOR_44":
				listMapingMarkColor.put("MARK_COLOR_44", HSSFColor.HSSFColorPredefined.CORAL.getIndex());
			case "MARK_COLOR_45":
				listMapingMarkColor.put("MARK_COLOR_45", HSSFColor.HSSFColorPredefined.ROYAL_BLUE.getIndex());
			case "MARK_COLOR_46":
				listMapingMarkColor.put("MARK_COLOR_46",
						HSSFColor.HSSFColorPredefined.LIGHT_CORNFLOWER_BLUE.getIndex());
			case "MARK_COLOR_47":
				listMapingMarkColor.put("MARK_COLOR_47", HSSFColor.HSSFColorPredefined.TAN.getIndex());
			default:
			}
			// System.out.println(temp);
		});

		return listMapingMarkColor;
	}

	private void createheader(Sheet sheet, List<ColumnInfo> isNameTable, Row row, HSSFWorkbook workbook) {
		Cell cell;
		for (int i = 0; i < isNameTable.size(); i++) {
			cell = row.createCell(i, CellType.STRING);
			cell.setCellValue(isNameTable.get(i).getName());
			CellStyle style1 = workbook.createCellStyle();
			style1.setBorderTop(BorderStyle.MEDIUM);
			style1.setBorderBottom(BorderStyle.MEDIUM);
			style1.setBorderLeft(BorderStyle.MEDIUM);
			style1.setBorderRight(BorderStyle.MEDIUM);
			cell.setCellStyle(style1);
			for (int x = 0; x < sheet.getRow(0).getPhysicalNumberOfCells(); x++) {
				sheet.autoSizeColumn(x);
			}
		}
	}

	private void createValiheader(Sheet sheet, List<ColumnInfo> isNameTable, Row row, HSSFCellStyle style) {
		Cell cell;
		for (int i = 0; i < isNameTable.size(); i++) {
			cell = row.createCell(i, CellType.STRING);
			cell.setCellValue(isNameTable.get(i).val);
			cell.setCellStyle(style);
		}
	}

	public byte[] outputFieSql(List<String> inputSQL) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			for (String x : inputSQL) {
				bos.write(x.getBytes());
				bos.write(";\n".getBytes());
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			bos.close();
		}
		return bos.toByteArray();
	}

}
