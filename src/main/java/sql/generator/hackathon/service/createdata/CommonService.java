package sql.generator.hackathon.service.createdata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.Cond;
import sql.generator.hackathon.model.Condition;
import sql.generator.hackathon.model.ObjectCommonCreate;
import sql.generator.hackathon.model.ObjectGenate;
import sql.generator.hackathon.model.ParseObject;
import sql.generator.hackathon.model.TableSQL;
import sql.generator.hackathon.model.createdata.InfoMappingTableColumnObject;
import sql.generator.hackathon.model.createdata.constant.Constant;
import sql.generator.hackathon.service.ExecuteDBSQLServer;

@Service
public class CommonService {

	public ObjectCommonCreate objCommon;
	
	private ExecuteDBSQLServer dbService;
	

	public void init(ExecuteDBSQLServer dbService, ObjectGenate objectGenate, 
			ParseObject parseObject) throws Exception {
		objCommon = new ObjectCommonCreate();
		String typeConnection = objectGenate.getInfoDatabase().getType();
		objCommon.setObjectGenate(objectGenate);
		objCommon.setListTableName(getListTableName(parseObject.getListTableSQL()));
		objCommon.setHasReFormat(new HashSet<>());
		
		this.dbService = dbService;
		
		Map<String, List<ColumnInfo>> tableInfo;
		objCommon.setListTableAlias(getListTableAlias(parseObject.getListTableSQL()));
		if (typeConnection.equalsIgnoreCase(Constant.STR_NO_CONNECTION)) {
			tableInfo = getInfoTableWithoutConnect(parseObject.getListTableSQL());
		} else {
			tableInfo = dbService.getInforTable(objCommon.getObjectGenate().getInfoDatabase().getSchema(), 
							objCommon.getListTableName(), objCommon.getListTableAlias());
		}
		objCommon.setTableInfo(tableInfo);
		processReformatDataType(tableInfo, parseObject.getListTableSQL());
	}
	
	public  ColumnInfo getColumnInfo(String tableName, String columnName) {
		List<ColumnInfo> columnsInfo = objCommon.getTableInfo().get(tableName);
		List<ColumnInfo> res = columnsInfo.stream().filter(x -> x.getName().equals(columnName))
								.collect(Collectors.toList());
		
		if (res.size() != 1) {
			throw new IllegalStateException();
		}
		return res.get(0);
	}
	
	public void processReFormatDataTypeForMapping(Map<String, List<Cond>> columnMap) {
		if (!objCommon.getObjectGenate().getInfoDatabase().getType().equalsIgnoreCase(Constant.STR_NO_CONNECTION)) {
			return;
		}
		for (Map.Entry<String, List<Cond>> e : columnMap.entrySet()) {
			String tableAliasColumnName = getTableAliasColumnName(e.getKey());
			
			String[] arrTableColumnName = getArrInColumns(tableAliasColumnName); 
			String tableName = arrTableColumnName[0];
			String colName = arrTableColumnName[2];
			String tableColumnName = tableName + Constant.STR_DOT + colName;
			ColumnInfo t = getColumnInfo(tableName, colName);
			if (objCommon.getHasReFormat().contains(tableColumnName)) {
				String typeName = t.getTypeName();
				String typeValue = t.getTypeValue();
				for (Cond cond : e.getValue()) {
					String[] arrTableColumnName1 = getArrInColumns(cond.value);
					String[] arrTableColumnName2 = getArrInColumns(cond.rightValue);
					ColumnInfo t1 = getColumnInfo(arrTableColumnName1[0], arrTableColumnName1[2]);
					if (t1 != null) {
						t1.setTypeName(typeName);;
						t1.setTypeValue(typeValue);
					}
					ColumnInfo t2 = getColumnInfo(arrTableColumnName2[0], arrTableColumnName2[2]);
					if (t2 != null) {
						t2.setTypeName(typeName);;
						t2.setTypeValue(typeValue);
					} 
				}
			}
		}
	}
	
	/**
	 * Check is Composite key in table
	 * @param tableName
	 * @return
	 */
	public  boolean isCompositeKey(String tableName) {
		boolean res = false;
		for (Entry<String, List<ColumnInfo>> x : objCommon.getTableInfo().entrySet()) {
			String innerTableName = x.getKey();
			if (innerTableName.equals(tableName)) {
				List<ColumnInfo> columnInfo = x.getValue();
				long cnt = columnInfo.stream().filter(y -> y.isKey()).count();
				if (cnt > 1) {
					res = true;
					break;
				}
			}
		};
		return res;
	}
	
	public <T> List<T> processMergeList(List<T> list1, List<T> list2){
		Set<T> set = new HashSet<>();
		if (list1 == null) {
			return list2;
		}
		
		if (list2 == null) {
			return list1;
		}
		
		set.addAll(list1.stream().collect(Collectors.toSet()));
		set.addAll(list2.stream().collect(Collectors.toSet()));
		return set.stream().collect(Collectors.toList());
	}
	
	/**
	 * Get table info for NoConnection
	 * @param tables
	 * @return
	 */
	private  Map<String, List<ColumnInfo>> getInfoTableWithoutConnect(List<TableSQL> tables) {
		Map<String, List<ColumnInfo>> res = new HashMap<>();
		for (TableSQL table : tables) {
			List<ColumnInfo> listColInfo = new ArrayList<>();
			Set<String> listColumn = new HashSet<>();
			for (Condition condition : table.getCondition()) {
				String[] tableColName = getArrInColumns(condition.getLeft());
				if (listColumn.contains(tableColName[2])) {
					continue;
				}
				listColumn.add(tableColName[2]);
				ColumnInfo colInfo = new ColumnInfo(tableColName[2], "", Constant.STR_TYPE_CHAR, String.valueOf(Constant.DEFAULT_LENGTH_TYPE_CHAR));
				colInfo.setTableAlias(tableColName[0]);
				listColInfo.add(colInfo);
			}
			if (res.containsKey(table.getTableName())) {
				List<ColumnInfo> columnsCanAdd = new ArrayList<>();
				List<ColumnInfo> currentColumns = res.get(table.getTableName());
				for (ColumnInfo c : listColInfo) {
					boolean flg = true;
					for (ColumnInfo current : currentColumns) {
						if (current.getName().equals(c.getName())) {
							flg = false;
							break;
						}
					}
					if (flg) {
						columnsCanAdd.add(c);
					}
				}
				for (ColumnInfo c : columnsCanAdd) {
					currentColumns.add(c);
				}
			} else {
				res.put(table.getTableName(), listColInfo);
			}
		}
		return res;
	}
	
	/**
	 * Get table name and column name
	 * 
	 * @param input 
	 * @return String[3], String[0] = tableName, String[1] = aliasName ,String[2] = columnName
	 */
	public  String[] getArrInColumns(String input) {
		String[] res = new String[3];
		if (input.indexOf(Constant.STR_DOT) != -1) {
			String[] tmp = input.split("\\" + Constant.STR_DOT);
			if (tmp.length != 3) {
				res[0] = tmp[0];
				res[2] = tmp[1];
			} else {
				res = tmp;
			}
		} else {
			res[2] = input;
		}
		return res;
	}
	
	
	/**
	 * Remove all specify character in string origin
	 * 
	 * @param specifyStr String of specify character to remove.
	 * @param origin
	 * @return String without character in specifyStr.
	 */
	public  String removeSpecifyCharacter(String specifyStr, String origin) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < origin.length(); ++i) {
			if (!specifyStr.contains("" + origin.charAt(i))) {
				sb.append(origin.charAt(i));
			}
		}
		return sb.toString();
	}
	
	public  String removeSpecifyCharacterFirstLastStr(char needRemove, String origin) {
		String res = "";
		char[] arr = origin.toCharArray();
		if (arr == null) {
			return origin;
		}
		if (arr[0] == needRemove) {
			if (arr[arr.length - 1] == needRemove) {
				res = origin.substring(1, arr.length - 1);
			} else {
				res = origin.substring(1);
			}
		} else {
			if (arr[arr.length - 1] == needRemove) {
				res = origin.substring(0, arr.length - 1);
			}
		}
		return res.isEmpty() ? origin : res;
	}
	
	/**
	 * Process Gen value
	 */
	public  List<String> processGenValue(String dataType, int len, String valGreater, String valLess) {
		List<String> res = new ArrayList<>();
		boolean hasLess = !valLess.isEmpty();
		
		// Calculator increase or decrease?
		boolean isIncrement = false;
		if (valGreater.isEmpty() && valLess.isEmpty()) {
			isIncrement = true;
		} else if (!valGreater.isEmpty()) {
			isIncrement = true;
		}
		String curVal = isIncrement ? valGreater.isEmpty() 
									? processGenValueWithLength(dataType, len) 
									: valGreater : valLess;
		
		for (int i = 1; i <= Constant.LIMIT_GEN_VALUE; ++i) {
			res.add(curVal);
			String newVal = "";
			if (dataType.equals(Constant.STR_TYPE_DATE)) {
				newVal = processGenValueTypeDate(isIncrement, curVal);
				if (hasLess && isIncrement) {
					Date t1 = convertStringToDate(valLess);
					Date t2 = convertStringToDate(newVal);
					
					// New value large than limit value
					if (t2.compareTo(t1) > 0) {
						break;
					}
				}
			} else if (dataType.equals(Constant.STR_TYPE_NUMBER)) {
				newVal = processGenValueTypeNumber(isIncrement, curVal);
				Integer t2 = convertStringToInt(newVal);
				
				// When greater len stop!
				if (newVal.length() > len || t2 < 0) {
					break;
				}
				if (hasLess && isIncrement) {
					Integer t1 = convertStringToInt(valLess);
					
					// New value large than limit value
					if (t2 > t1) {
						break;
					}
				}
			} else if (dataType.equals(Constant.STR_TYPE_CHAR)) {
				newVal = processGenValueTypeChar(isIncrement, curVal);
				// When greater len stop!
				if (newVal.length() > len) {
					break;
				}
			}
			curVal = newVal;
		}
		return res;
	}
	
	public  String processRemoveApostrophe(String input) {
		return removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, input);
	}
	
	public  List<String> processRemoveApostrophe(List<String> inputs) {
		List<String> res = new ArrayList<>();
		for (String input : inputs) {
			res.add(processRemoveApostrophe(input));
		}
		return res;
	}
	/**
	 * All data type will to 3 dataType below
	 * number - char - date
	 * @param dataType
	 * @return
	 */
	public  String getCommonDataType(String dataType) {
		String res = "";
		switch(dataType) {
		case "number":
		case "int":
		case "bigint":
			res = "number";
			break;
		case "char":
		case "nchar":
		case "varchar":
		case "nvarchar":
			res = "char";
			break;
		case "date":
			res = "date";
			break;
		default:
			res = "unknow";
		}
		return res;
	}
	
	/**
	 * Convert String to int
	 * When Numberformatexception will get default length
	 * @param val
	 * @return
	 */
	public  int convertLength(String val) {
		int len;
		try {
			len = Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return Constant.DEFAULT_LENGTH;
		}
		return len;
	}
	
	/**
	 * Convert string to date
	 * Format will get from input value
	 */
	@SuppressWarnings("finally")
	public  Date convertStringToDate(String input) {
		Date res = new Date();
		try {
			input = removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, input);
			String format = readFormatDate(input);
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			res = sdf.parse(input);
		} catch (java.text.ParseException e) {
			return res;
		} finally {
			// Get current date
			SimpleDateFormat sdf = new SimpleDateFormat(Constant.DEFAULT_FORMAT_DATE);
			Date date = new Date();
			try {
				return sdf.parse(sdf.format(date));
			} catch (java.text.ParseException e) {
				return res;
			}
		}
	}

	/**
	 * Convert Date to string 
	 * Format will get from input value
	 */
	public  String convertDateToString(String format, Date input) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(input);
	}
	
	/**
	 * Convert string to int
	 */
	public  int convertStringToInt(String input) {
		int res;
		try {
			res = Integer.parseInt(removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, input));
		} catch (NumberFormatException e) {
			return Constant.DEFAULT_LENGTH;
		}
		return res;
	}
	
	
	/**
	 * Read format date from input string
	 * @param input
	 * @return
	 */
	public  String readFormatDate(String input) {
		// TODO
		return Constant.DEFAULT_FORMAT_DATE;
	}

	public  String[] StringToArrWithRegex(String regex, String input) {
		return input.split("\\" + regex);
	}
	
	public  String getTableAliasColumnName(String input) {
		String[] arr = StringToArrWithRegex(Constant.STR_DOT, input);
		Set<String> existsName = new HashSet<>();
		StringBuilder res = new StringBuilder();
		for (String c : arr) {
			if (!existsName.contains(c)) {
				if (res.length() != 0) {
					res.append(Constant.STR_DOT);
				}
				res.append(c);;
			}
			existsName.add(c);
		}
		return res.toString();
	}
	
	public  String getTableAliasName(String input) {
		String[] arr = StringToArrWithRegex(Constant.STR_DOT, input);
		if (arr[0].equals(arr[1])) {
			return arr[0];
		}
		return input;
	}
	
	/**
	 * @param dataType
	 * @param len
	 * @return
	 */
	public  String processGenValueWithLength(String dataType, int len) {
		if (dataType.equals(Constant.STR_TYPE_DATE)) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constant.DEFAULT_FORMAT_DATE);  
			LocalDateTime now = LocalDateTime.now();  
			return dtf.format(now);  
		}
		
		StringBuilder res = new StringBuilder();
		if (dataType.equals(Constant.STR_TYPE_NUMBER)) {
			res.append(Constant.DEFAULT_NUMBER);
		} else if (dataType.equals(Constant.STR_TYPE_CHAR)) {
			res.append(Constant.DEFAULT_CHAR);
		}
		return res.toString();
	}
	
	/**
	 * @param type (++, --)
	 * @param curVal current value
	 * @return String new value after (++, --)
	 */
	private  String processGenValueTypeDate(boolean isIncrease, String curVal) {
		Date curD;
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(Constant.DEFAULT_FORMAT_DATE);
		try {
			curD = sdf.parse(curVal);
			c.setTime(curD);
		} catch(ParseException e) {
		} finally {
			
		}
		if (isIncrease) {
			c.add(Calendar.DATE, 1);
		} else {
			// Decrease
			c.add(Calendar.DATE, -1);
		}
		curD = c.getTime();
		
		return sdf.format(curD);
	}
	
	/**
	 * @param type (++, --)
	 * @param curVal current value
	 * @return String new value after (++, --)
	 */
	private  String processGenValueTypeNumber(boolean isIncrease, String curVal) {
		String val = "";
		try {
			Integer i = Integer.parseInt(curVal);
			if (isIncrease) {
				i++;
			} else {
				i--;
			}
			val = String.valueOf(i);
		} catch (NumberFormatException e) {
			return String.valueOf(Constant.DEFAULT_NUMBER);
		}
		return val;
	}
	
	/**
	 * Just confirm in normal character [26 character]
	 * @param type (++, --)
	 * @param curVal current value
	 * @return String new value after (++, --)
	 */
	private  String processGenValueTypeChar(boolean isIncrement, String curVal) {
		char[] chr = new char[26];
		for (int i = 0; i < 26; ++i) {
			chr[i] = (char) (Constant.DEFAULT_CHAR + i); 
		}
		
		if (curVal == null || curVal.isEmpty()) {
			curVal = String.valueOf(Constant.DEFAULT_CHAR);
		}
		
		char[] curChar = curVal.toCharArray();
		
		String res;
		if (isIncrement) {
			res = processGenValueTypeCharIncrement(curChar);
		} else {
			res = processGenValueTypeCharDecrement(curChar);
		}
		return res;
	}
	
	/**
	 * Increase
	 * 'z' -> 'aa'
	 * @param curChar
	 */
	private  String processGenValueTypeCharIncrement(char[] curChar) {
		int remain = 0;
		int length = curChar.length;
		for (int i = length - 1; i >= 0; --i) {
			char c = curChar[i];
			if (c == Constant.CHAR_Z) {
				remain = 1;
				if (i == 0) {
					return repeat(Constant.DEFAULT_CHAR, length + 1);
				}
			} else {
				remain = 0;	
				curChar[i] = (char) (c + 1);
			}
			
			if (remain == 0) {
				break;
			}
		}
		return String.valueOf(curChar);
	}
	
	/**
	 * Decrease
	 * 'abcdf' -> 'abcde'
	 * @param curChar
	 * @return
	 */
	private  String processGenValueTypeCharDecrement(char[] curChar) {
		int remain = 0;
		int length = curChar.length;
		for (int i = length - 1; i >= 0; --i) {
			char c = curChar[i];
			if (c == Constant.DEFAULT_CHAR) {
				remain = 1;
				if (i == 0) {
					return repeat(Constant.CHAR_Z, length - 1);
				}
			} else {
				remain = 0;	
				curChar[i] = (char) (c - 1);
			}
			if (remain == 0) {
				break;
			}
		}
		return String.valueOf(curChar);
	}
	
	/**
	 * Repeat character
	 * @param character need repeat
	 * @param length need repeat
	 * @return String repeated
	 */
	private  String repeat(char c, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; ++i) {
			sb.append("" + c);
		}
		return sb.toString();
	}
	
	/**
	 * Get list tableName
	 * @param tables
	 * @return
	 */
	private  List<String> getListTableName(List<TableSQL> tables) {
		Set<String> res = tables.stream().map(x -> x.tableName).collect(Collectors.toSet());
		return res.stream().collect(Collectors.toList());
	}
	
	private  void processReformatDataType(Map<String, List<ColumnInfo>> tableInfo, List<TableSQL> tables) {
		Map<String, InfoMappingTableColumnObject> mappingTableColumn = new HashMap<>();
		for (TableSQL table : tables) {
			for (Condition condition : table.getCondition()) {
				if (condition.getRight() != null && condition.getRight().startsWith(Constant.STR_KEYS)) {
					continue;
				}
				String[] tableColName = getArrInColumns(condition.getLeft());
				String tableColumnName = table.getTableName() + Constant.STR_DOT + tableColName[2];
				List<String> listExpression;
				List<String> listValue;
				if (mappingTableColumn.containsKey(tableColumnName)) {
					listExpression = mappingTableColumn.get(tableColumnName).getListExpression();
					listValue = mappingTableColumn.get(tableColumnName).getListValue();
				} else {
					InfoMappingTableColumnObject infoMappingTableColumnobj = new InfoMappingTableColumnObject();
					listExpression = new ArrayList<>();
					listValue = new ArrayList<>();
					infoMappingTableColumnobj.setListExpression(listExpression);
					infoMappingTableColumnobj.setListValue(listValue);
					mappingTableColumn.put(tableColumnName, infoMappingTableColumnobj);
				}
				if (condition.getRight() != null) {
					listValue.add(condition.getRight());
				} else {
					if (condition.getListRight() != null) {
						listValue.addAll(condition.getListRight());
					}
				}
				listExpression.add(condition.getExpression());
			}
		}
		for (Map.Entry<String, InfoMappingTableColumnObject> e : mappingTableColumn.entrySet()) {
			String[] arrTableColumnName = getArrInColumns(e.getKey());
			InfoMappingTableColumnObject infoMappingTableColumnobj = e.getValue();
			List<String> listExpression = infoMappingTableColumnobj.getListExpression();
			List<String> listValue = infoMappingTableColumnobj.getListValue();
			ColumnInfo columnInfo = getColumnInfo(arrTableColumnName[0], arrTableColumnName[2]);
			String typeName = "";
			String typeValue = "";
			boolean flgReFormat = false;
			for (String expression : listExpression) {
				if (expression.equals(Constant.EXPRESSION_GREATER) || 
						expression.equals(Constant.EXPRESSION_GREATER_EQUALS) ||
						expression.equals(Constant.EXPRESSION_LESS) ||
						expression.equals(Constant.EXPRESSION_LESS_EQUALS)) {
					typeName = Constant.STR_TYPE_NUMBER;
					typeValue = String.valueOf(Constant.DEFAULT_LENGTH);
					flgReFormat = true;
					break;
				}
			}
			
			for (String value : listValue) {
				if (flgReFormat) {
					break;
				}
				if (isNumber(value)) {
					typeName = Constant.STR_TYPE_NUMBER;
					typeValue = String.valueOf(Constant.DEFAULT_LENGTH);
					flgReFormat = true;
					break;
				} else if (isDate(value)) {
					typeName = Constant.STR_TYPE_DATE;
					typeValue = null;
					flgReFormat = true;
					break;
				} else {
				}
			}
			
			if (flgReFormat) {
				columnInfo.setTypeName(typeName);
				columnInfo.setTypeValue(typeValue);
				String tableColumnName = arrTableColumnName[0] + Constant.STR_DOT + arrTableColumnName[2];
				objCommon.getHasReFormat().add(tableColumnName);
			}
		}
	}
	
	/**
	 * Check is Number
	 */
	private  boolean isNumber(String origin) {
		try {
			Integer.parseInt(removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, origin));
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Check is Number
	 */
	private boolean isDate(String origin) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			sdf.parse(removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, origin));
		} catch (ParseException e) {
			return false;
		}
		return true;
	}
	
	private Map<String, List<String>> getListTableAlias(List<TableSQL> tables) {
		Map<String, List<String>> res = new HashMap<>(); 
		for (TableSQL table : tables) {
			String tableName = table.getTableName();
			List<Condition> conditions = table.getCondition();
			String aliasName = table.getAlias();
			if (!res.containsKey(tableName)) {
				res.put(tableName, new ArrayList<>());
			}
			if (!res.get(tableName).contains(aliasName)) {
				res.get(tableName).add(aliasName);
			}
			for (Condition condition : conditions) {
				String[] tableColName = getArrInColumns(condition.getLeft());
				if (!res.containsKey(tableName)) {
					res.put(tableName, new ArrayList<>());
				}
				if (!res.get(tableName).contains(tableColName[0])) {
					res.get(tableName).add(tableColName[0]);
				}
			}
		}
		return res;
	}
}
