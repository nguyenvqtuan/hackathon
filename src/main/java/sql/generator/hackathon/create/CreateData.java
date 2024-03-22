package sql.generator.hackathon.create;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sql.generator.hackathon.exception.NotFoundValueSQLException;
import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.Cond;
import sql.generator.hackathon.model.Condition;
import sql.generator.hackathon.model.CreateDataObj;
import sql.generator.hackathon.model.CreateObject;
import sql.generator.hackathon.model.InforTableReferFK;
import sql.generator.hackathon.model.NodeColumn;
import sql.generator.hackathon.model.TableSQL;
import sql.generator.hackathon.service.CommonCreateService;
import sql.generator.hackathon.service.CreateService;
import sql.generator.hackathon.service.ExecuteDBSQLServer;
import sql.generator.hackathon.service.ServerFaker;

@Service
public class CreateData {

	// Priority of operator
	private static Map<String, Integer> priorityOfOperator = new HashMap<>();
	{
		priorityOfOperator.put("=", 1);
		priorityOfOperator.put("IN", 2);
		priorityOfOperator.put("LIKE", 3);
		priorityOfOperator.put(">=", 4);
		priorityOfOperator.put("<=", 5);
		priorityOfOperator.put(">", 6);
		priorityOfOperator.put("<", 7);
		priorityOfOperator.put("NOT IN", 8);
		priorityOfOperator.put("!=", 9);
		priorityOfOperator.put("<>", 10);
	}

	private static List<String> operatorInLike = new ArrayList<>();
	{
		operatorInLike.add("%");
		operatorInLike.add("_");
	}
	
	private String SCHEMA_NAME = "admindb";
	
	private static String locale;
	
	@Autowired
	private CommonCreateService commonService;
	
	@Autowired
	private ServerFaker fakerService;

	// Save Key exists
	private List<TableSQL> tables = new ArrayList<>();
	private Map<String, List<String>> keys = new HashMap<>();
	
	private Map<String, List<String>> keysFormat;

	// Key = tableName.colName
	private Map<String, CreateDataObj> saveCalcObj;
	
	private Map<String, String> aliasNameTable;
	
	// Data current values
	// TableName => List ColumnInfo
	private Map<String, List<ColumnInfo>> tableData;
	
	private Map<String, InforTableReferFK> foreignKeyNotExistsInmainTable;

	// tableName1.colName1 - tableName2.colName2 => [operator1, operator2] 
	private Map<String, String[]> infoCol;
	
	private CreateService createService;
	
	private ExecuteDBSQLServer dbServer;
	
	private int idxColor = 1;
	
	public CreateData() {
	}
	
	public void init(String type, ExecuteDBSQLServer dbServer, String schema, List<String> listTables, List<TableSQL> tables, 
			Map<String, List<String>> keys, Map<String, TableSQL> fulltableInfo, String locale) throws Exception {
		this.dbServer = dbServer;
		this.locale = locale;
		SCHEMA_NAME = schema;
		this.tables = tables;
		this.keys = keys;
		commonService.init(type, listTables, schema, dbServer, fulltableInfo);
		commonService.exeGetTableInfo(tables);
	}
	
	public void init(){
		keysFormat = new HashMap<>();
		
		saveCalcObj = new HashMap<>();
		// alias.Name => <tableName.columnName, operator>
		infoCol = new HashMap<>();

		aliasNameTable = new HashMap<>();
		
		// Data current values
		// TableName => List ColumnInfo
		tableData = new HashMap<>();
		
		foreignKeyNotExistsInmainTable = new HashMap<>();
		
		// markcolor
		idxColor = 1;
	}
	
	public CreateObject multipleCreate(Map<String, List<List<ColumnInfo>>> dataClient, 
			int row, boolean type) throws Exception {
		Map<String, List<List<ColumnInfo>>> response = new HashMap<>();
		CreateObject createObj = new CreateObject();
		// Insert multiple row
		for (int i = 0; i < row; ++i) {
			Map<String, List<ColumnInfo>> dataOneRow = create(dataClient, type, i);
			for (Map.Entry<String, List<ColumnInfo>> m : dataOneRow.entrySet()) {
				String tableName = m.getKey();
				List<List<ColumnInfo>> t;
				if (response.containsKey(tableName)) {
					t = response.get(tableName);
				} else {
					t = new ArrayList<>();
					response.put(tableName, t);
				}
				t.add(m.getValue());
			}
		}
		
		processWithForeignKey(response);
		
		createObj.listData = response;
		
		// Process add listMarkColor
		createObj.listMarkColor = processAddListMarkColor();
		
		return createObj;
	}
	
	/**
	 * Execute create data after parse object
	 * @throws Exception 
	 */
	public Map<String, List<ColumnInfo>> create(Map<String, List<List<ColumnInfo>>> dataClient,
			boolean type, int idxRow) throws Exception {
		
		init();
		
		int sz = tables.size();
		for (int i = 0; i < sz; ++i) {
			exeEachTable(tables.get(i));
		}
		
		// Get infoCol
		for (int i = 0; i < sz; ++i) {
			for (Condition cd : tables.get(i).getCondition()) {
				if (cd.getRight() == null || !cd.getRight().startsWith("KEY")) {
					continue;
				}
				String tableColName = tables.get(i).getTableName() + "." + commonService.getTableAndColName(cd.getLeft())[1];
				String tableColName2 = "";
				for (String t : keysFormat.get(cd.getRight())) {
					if (!t.equals(tableColName)) tableColName2 = commonService.getTableAndColName(t)[0] + "." + commonService.getTableAndColName(t)[1];
				}
				processCalInfoCol(tableColName, tableColName2, cd.expression);
			}
		}

		// Get column Mapping (Key1 -> Key2, Key2 -> Key) in keys
		Map<String, Set<String>> colMapping = getMappingColumn();
		// Get all mapping of 1 column (Key1 -> key2,key3,key4)
		Map<String, List<Cond>> columnMap  = getAllMappingColum(colMapping);
		
		// Execute reformat dataType for columnInfo
		if (commonService.getCommonCreateObj().getType() == 0) {
			exeReFormatDataType(columnMap);
		}
		
		// execute calculator for mapping key.
		exeCalcForMappingKey(columnMap);
		
		// Create last data for column!
		createLastData();
		
		Map<String, List<ColumnInfo>> lst = processInsert(dataClient, idxRow);
		
		// Insert table
		if (type) {
			for (Map.Entry<String, List<ColumnInfo>> e : lst.entrySet()) {
				createService.insert(e.getKey(), e.getValue());
			}
		}
		
		return lst;
	}
	

	private void exeEachTable(TableSQL table) {
		String tableName = table.tableName;
		aliasNameTable.put(tableName, table.alias);
		
		// Init data for table
		tableData.put(tableName, new ArrayList<>());

		// Save cur value of column table
		// Key = table.column, Value = [operator, value, priority] in Where
		Map<String, List<String[]>> mapValOfColumn = new HashMap<>();

		// Read list condition
		List<Condition> conditions = table.condition;

		// Read all condition.
		int szCond = conditions.size();
		for (int i = 0; i < szCond; ++i) {
			if ((conditions.get(i).right == null && !conditions.get(i).listRight.isEmpty()) || !conditions.get(i).right.startsWith("KEY")) {
				// Normal case
				readValueForColumn(tableName, conditions.get(i), mapValOfColumn);
				continue;
			}

			String left = conditions.get(i).left;
			String operator = conditions.get(i).expression;
			String right = conditions.get(i).right;

			// Execute find valid value column = KEY
			// Find mapping = KEY of all column
			// Save to columnMap
			if (!keys.containsKey(right)) {
				System.out.println("Error not found KEY Mapping!");
				return;
			}

			String[] sp = commonService.getTableAndColName(left);
			
			// Format all Key aliasName.colName => tableName.colName
			List<String> valKey;
			if (keysFormat.containsKey(right)) {
				valKey = keysFormat.get(right);
			} else {
				valKey = new ArrayList<>();
				keysFormat.put(right, valKey);
			}
			String tableColName = tableName + "." + sp[1];
			valKey.add(tableColName);
			
			if (!saveCalcObj.containsKey(tableColName)) {
				CreateDataObj createObj = new CreateDataObj();
				createObj.getListOperator().add(operator);
				saveCalcObj.put(tableColName, createObj);
			} else {
				saveCalcObj.get(tableColName).getListOperator().add(operator);
			}
		}

		// Calculator Priority of condition.
		for (Map.Entry<String, List<String[]>> entry : mapValOfColumn.entrySet()) {
			List<String[]> t = entry.getValue();

			Collections.sort(t, new Comparator<String[]>() {
				@Override
				public int compare(String[] o1, String[] o2) {
					return Integer.parseInt(o1[2]) - Integer.parseInt(o2[2]);
				}
			});
		}

		// Check all condition in mapValOfColumn With key is tableName.columName
		// Calculator valid value for column
		// Key = tableName.colName, Value = List<Cond>
		Map<String, List<Cond>> validVal = calValidValueOfColumn(mapValOfColumn);
		for (Map.Entry<String, List<Cond>> e : validVal.entrySet()) {
			CreateDataObj createObj;
			if (saveCalcObj.containsKey(e.getKey())) {
				createObj = saveCalcObj.get(e.getKey());
			} else {
				createObj = new CreateDataObj();
				saveCalcObj.put(e.getKey(), createObj);
			}
			createObj.setValidValuesForColumn(e.getValue());
		}
	}

	/**
	 * Read value to mapValOfColumn to execute bridging case (Bắc cầu)
	 * 
	 * @param condition
	 * @param mapValueOfColumn
	 */
	private void readValueForColumn(String tableName, Condition condition,
			Map<String, List<String[]>> mapValOfColumn) {
		String col = condition.left;
		String operator = condition.expression;
		String val = condition.right;

		// TableName.columnName
		String fullColName = tableName + "." + commonService.getTableAndColName(col)[1];

		if (!saveCalcObj.containsKey(fullColName)) {
			CreateDataObj createObj = new CreateDataObj();
			createObj.getListOperator().add(operator);
			createObj.getListValue().add(val);
			saveCalcObj.put(fullColName, createObj);
		} else {
			if (saveCalcObj.get(fullColName).getListOperator().contains(operator)) {
				saveCalcObj.get(fullColName).getListOperator().add(operator);
			}
			saveCalcObj.get(fullColName).getListValue().add(val);
		}
		
		// Add all condition.
		List<String[]> t;
		if (mapValOfColumn.containsKey(fullColName)) {
			t = mapValOfColumn.get(fullColName);
		} else {
			// New condition
			t = new ArrayList<>();
			mapValOfColumn.put(fullColName, t);
		}
		if (!(operator.equals("NOT IN") || operator.equals("IN"))) {
			t.add(new String[] { operator, val, String.valueOf(priorityOfOperator.get(operator)) });
		} else {
			List<String> listRight = condition.listRight;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < listRight.size(); ++i) {
				sb.append(listRight.get(i));
				if (i != listRight.size() - 1) {
					sb.append(",");
				}
				saveCalcObj.get(fullColName).getListValue().add(listRight.get(i));
			}
			t.add(new String[] { operator, sb.toString(), String.valueOf(priorityOfOperator.get(operator)) });
		}
	}

	/**
	 * Calculator valid value for column
	 * Use for condition in where
	 * @return Map<String, List<Cond> Key = tableName.colName, Value = Valid value.
	 */
	private Map<String, List<Cond>> calValidValueOfColumn(Map<String, List<String[]>> mapValOfColumn) {
		Map<String, List<Cond>> m = new HashMap<>();
		for (Map.Entry<String, List<String[]>> entry : mapValOfColumn.entrySet()) {
			String key = entry.getKey();
			
			// Get dataType of column
			String tableName = commonService.getTableAndColName(key)[0];
			String colName = commonService.getTableAndColName(key)[1];
			String dataType = commonService.getDataTypeOfColumn(commonService.getColumnInfo(tableName, colName).getTypeName());

			// {operator, value, priority}
			// operator => (=, <>, !=, ..)
			// value => current value in condition where
			// Priority of current operator
			List<String[]> list = entry.getValue();
			int sz = list.size();
			List<Cond> tmpVal = new ArrayList<>();
			List<Cond> listCondIN = new ArrayList<>();
			boolean flgCheckCondIN = false;
			
			for (int i = 0; i < sz; ++i) {
				// String[operator, value, priority]
				String[] cur = list.get(i);
				String operator = cur[0];

				// When priority = 1 stop here
				if (operator.equals("=")) {
					if (saveCalcObj.containsKey(tableName + "." + colName)) {
						saveCalcObj.get(tableName + "." + colName).setLastEndValidValue(cur[1]);
					} else {
						CreateDataObj createObj = new CreateDataObj();
						createObj.setLastEndValidValue(cur[1]);
						saveCalcObj.put(tableName + "." + colName, createObj);
					}
					break;
				} else if (operator.equals("LIKE")) {
					String valLike = processConditionLike(cur[1]);
					if (saveCalcObj.containsKey(tableName + "." + colName)) {
						saveCalcObj.get(tableName + "." + colName).setLastEndValidValue(valLike);
					} else {
						CreateDataObj createObj = new CreateDataObj();
						createObj.setLastEndValidValue(valLike);
						saveCalcObj.put(tableName + "." + colName, createObj);
					}
					break;
				}
				
				// Set dataType for no-connection when meet >=, <, >, <=
				if (commonService.getCommonCreateObj().getType() == 0) {
					ColumnInfo innerCol = commonService.getColumnInfo(tableName, colName);
					dataType = setDataTypeForNoConnection(innerCol, operator, cur[1]);
				}

				switch (operator) {
				case "IN":
					flgCheckCondIN = true;
					listCondIN = addValueToColWithInOperator(cur, tmpVal);
					break;
				case "<=":
					try {
						addValueToColWithComparationsOperator(dataType, cur, tmpVal);
					} catch (ParseException e) {
						e.printStackTrace();
						System.out.println("Error parse from String to (Date OR Number) !");
					}
					break;
				case ">=":
					try {
						addValueToColWithComparationsOperator(dataType, cur, tmpVal);
					} catch (ParseException e) {
						e.printStackTrace();
						System.out.println("Error parse from String to (Date OR Number) !");
					}
					break;
				case "<":
					try {
						addValueToColWithComparationsOperator(dataType, cur, tmpVal);
					} catch (ParseException e) {
						e.printStackTrace();
						System.out.println("Error parse from String to (Date OR Number) !");
					}
					break;
				case ">":
					try {
						addValueToColWithComparationsOperator(dataType, cur, tmpVal);
					} catch (ParseException e) {
						e.printStackTrace();
						System.out.println("Error parse from String to (Date OR Number) !");
					}
					break;
				case "NOT IN":
					List<String> t;
					if (saveCalcObj.containsKey(key)) {
						t = saveCalcObj.get(key).getValueInValidOfColumn();
					} else {
						CreateDataObj createObj = new CreateDataObj();
						saveCalcObj.put(key, createObj);
						t = createObj.getValueInValidOfColumn();
					}
					addValueToColWithNotInOperator(dataType, cur, tmpVal, t);
					break;
				case "<>":
				case "!=":
					List<String> t2;
					if (saveCalcObj.containsKey(key)) {
						t2 = saveCalcObj.get(key).getValueInValidOfColumn();
					} else {
						CreateDataObj createObj = new CreateDataObj();
						saveCalcObj.put(key, createObj);
						t2 = createObj.getValueInValidOfColumn();
					}
					addValueToColWithDifferentOperator(dataType, cur, tmpVal, t2);
					break;
				default:
					System.out.println("Not valid case!");
					assert (false);
					break;
				}
			}
			// When has condition IN => get value of IN
			if (flgCheckCondIN) {
				m.put(key, listCondIN);
			} else {
				m.put(key, tmpVal);
			}
		}
		return m;
	}

	/**
	 * Execute add value for column with operator IN
	 * 
	 * @param String[]     cur (Each condition) [operator, value, priority]
	 * @param List<String> String[value] exactly value can save
	 */
	public List<Cond> addValueToColWithInOperator(String[] cur, List<Cond> values) {
		// Just get "123","123","123"
		String[] val = cur[1].split(",");
		for (int i = 0; i < val.length; ++i) {
			// Remove \" \" just get 123
			values.add(new Cond("=", removeSpecifyCharacter("\"'", val[i])));
		}
		
		List<Cond> res = new ArrayList<>();
		
		// Copy all values to new listCondIN
		for (int i = 0; i < values.size(); ++i) {
			res.add(values.get(i));
		}
		return res;
	}

	/**
	 * Execute add value for column with operator NOT IN
	 * 
	 * @param String[]     cur (Each condition) [operator, value, priority]
	 * @param List<String> String[value] exactly value can save
	 */
	public void addValueToColWithNotInOperator(String dataType, String[] cur, List<Cond> values, List<String> valNotIN) {
		// Just get "123","123","123"
		String[] val = cur[1].split(",");
		for (int i = 0; i < val.length; ++i) {
			// Remove \" \" just get 123
			String v = removeSpecifyCharacter("\"'", val[i]);
			
			// add value
			valNotIN.add(v);
			
			// Check in previous condition, and remove it if exists!
			Cond obj = new Cond("!=", v);
			Cond c = null;
			for (int j = 0; j < values.size(); ++j) {
				if (values.get(j).equals(obj)) {
					if (values.get(j).operator.equals("<=") || values.get(j).operator.equals(">=")) {
						c = values.get(j);
						break;
					}
				}
			}
			
			// Remove if contains
			if (values.contains(obj)) {
				// Check when not in mapping with less than or greater than!
				if (c != null) {
					
					Cond tmp = new Cond();
					tmp.operator = c.operator;

					if (c.operator.equals("<=")) {
						// Decrease - 1
						if (dataType.equals("number")) {
							tmp.value = genKeyWithTypeNumber(false, c.value);
						} else if (dataType.equals("date")) {
							tmp.value = genKeyWithTypeDate(false, c.value);
						}
					} else if (c.operator.equals(">=")) {
						// Increase + 1
						if (dataType.equals("number")) {
							tmp.value = genKeyWithTypeNumber(true, c.value);
						} else if (dataType.equals("date")) {
							tmp.value = genKeyWithTypeDate(true, c.value);
						}
					}
					values.add(tmp);
				}
				values.remove(obj);
			}
		}
	}

	/**
	 * Execute add value for column with operator <>, !=
	 * 
	 * @param String[]     cur (Each condition) [operator, value, priority]
	 * @param List<String> String[value] exactly value can save
	 */
	public void addValueToColWithDifferentOperator(String dataType, String[] cur, List<Cond> values, List<String> valNotIn) {
		String v = removeSpecifyCharacter("\"'", cur[1]);
		
		// Add value to not in table.column!
		valNotIn.add(v);
		// Check in previous condition, and remove it if exists!
		Cond obj = new Cond("!=", v);
		Cond c = null;
		for (int j = 0; j < values.size(); ++j) {
			if (values.get(j).equals(obj)) {
				if (values.get(j).operator.equals("<=") || values.get(j).operator.equals(">=")) {
					c = values.get(j);
					break;
				}
			}
		}
		
		// Check if exists in previous condition then remove it!
		if (values.contains(obj)) {
			
			if (c != null) {

				Cond tmp = new Cond();
				tmp.operator = c.operator;
				
				if (c.operator.equals("<=")) {
					// Decrease - 1
					if (dataType.equals("number")) {
						tmp.value = genKeyWithTypeNumber(false, c.value);
					} else if (dataType.equals("date")) {
						tmp.value = genKeyWithTypeDate(false, c.value);
					}
				} else if (c.operator.equals(">=")) {
					// Increase + 1
					if (dataType.equals("number")) {
						tmp.value = genKeyWithTypeNumber(true, c.value);
					} else if (dataType.equals("date")) {
						tmp.value = genKeyWithTypeDate(true, c.value);
					}
				}
				values.add(tmp);
			}
			
			values.remove(obj);
		}
	}

	/**
	 * Execute add value for column with operator <=, >=, <, > Just apply for data
	 * type is DATE OR NUMBER
	 * 
	 * @param data type of value. (date, number, char)
	 * @param String[] cur (Each condition) [operator, value, priority]
	 * @param List<String> String[value] valid value can save
	 * @throws ParseException
	 */
	public void addValueToColWithComparationsOperator(String type, String[] cur, List<Cond> values)
			throws ParseException {
		String operator = cur[0];
		String strVal = removeSpecifyCharacter("'", cur[1]);
		boolean flgAdd = true;
		
		// Convert to date when type = date
		if (type.equals("date")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date curD = parseStringToDate(strVal);
			
			// Comment for execute case multiple between
			Queue<Cond> toExploder = new LinkedList<>();
			
			for (int i = 0; i < values.size(); ++i) {
				toExploder.add(values.get(i));
			}
			
			while (!toExploder.isEmpty()) {
				Cond cond = toExploder.poll();
				Date ld = parseStringToDate(cond.value);
				
				if (!cond.operator.equals("=")) {
					continue;
				}
				
				// Just remove all element > curD
				if (operator.equals("<=")) {
					if (ld.compareTo(curD) < 0) {
						flgAdd = false;
						values.remove(cond);
						break;
					}
					// Just remove all element > curD
				} else if (operator.equals(">=")) {
					if (ld.compareTo(curD) > 0) {
						flgAdd = false;
						values.remove(cond);
						break;
					}
					// Just remove all element > curD
				} else if (operator.equals("<")) {
					if (ld.compareTo(curD) <= 0) {
						flgAdd = false;
						values.remove(cond);
						break;
					}
				} else if (operator.equals(">")) {
					if (ld.compareTo(curD) >= 0) {
						flgAdd = false;
						values.remove(cond);
						break;
					}
				} else {
					// TODO
				}
			}
			
			// Not operator <= and >=
			if (!(operator.equals("<=") || operator.equals(">="))) {
				Calendar c = Calendar.getInstance();
				c.setTime(curD);
				// current Date + 1
				if (operator.equals(">")) {
					operator = ">=";
					c.add(Calendar.DATE, 1);
					// current Date - 1
				} else {
					operator = "<=";
					c.add(Calendar.DATE, -1);
				}
				curD = c.getTime();
			}
			
			// Convert date time to string
			strVal = sdf.format(curD);
		} else if (type.equals("number")) {
			// Convert to long
			long curV = Long.parseLong(cur[1]);

			// Comment for execute case multiple between
			Queue<Cond> toExploder = new LinkedList<>();
			
			for (int i = 0; i < values.size(); ++i) {
				toExploder.add(values.get(i));
			}

			while (!toExploder.isEmpty()) {
				Cond cond = toExploder.poll();
				long innerV = Long.parseLong(cond.value);
				if (!cond.operator.equals("=")) {
					continue;
				}
				
				// Search in list to remove add value > this value;
				if (operator.equals("<=")) {
					if (innerV < curV) {
						values.remove(cond);
						flgAdd = false;
						break;
					}
					// Search in list to remove add value < this value;
				} else if (operator.equals(">=")) {
					if (innerV > curV) {
						values.remove(cond);
						flgAdd = false;
						break;
					}
				} else if (operator.equals("<")) {
					if (innerV <= curV) {
						values.remove(cond);
						flgAdd = false;
						break;
					}
				} else if (operator.equals(">")) {
					if (innerV >= curV) {
						values.remove(cond);
						flgAdd = false;
						break;
					}
				} else {
					// TODO
					// Other operator
				}
			}
			
			// Not operator <= and >=
			if (!(operator.equals("<=") || operator.equals(">="))) {
				// currentValue - 1
				if (operator.equals(">")) {
					operator = ">=";
					strVal = String.valueOf(curV + 1);
					// currentValue + 1
				} else {
					operator = "<=";
					strVal = String.valueOf(curV - 1);
				}
			}
		} else {
			// TODO
			// Other data type?
		}
		// Add new element
		// With character "\' ?
		// [Operator, current value]
		if (flgAdd) {
			values.add(new Cond(operator, strVal));
		}
	}
	
	/**
	 * Read Mapping in keys With each key add 2 mapping
	 * 
	 * @return Map<String, String> Key1 - Key2, Key2 - Key1
	 */
	private Map<String, Set<String>> getMappingColumn() {
		Map<String, Set<String>> m = new HashMap<>();
		for (Map.Entry<String, List<String>> e : keysFormat.entrySet()) {
			List<String> v = e.getValue();
			for (int i = 0; i < v.size(); ++i) {
				Set<String> t;
				if (m.containsKey(v.get(i))) {
					t = m.get(v.get(i));
				} else {
					t = new HashSet<>();
					m.put(v.get(i), t);
				}
				// Add other.
				t.add(v.get(i == 0 ? 1 : 0));
			}
		}
		return m;
	}
	
	/**
	 * Create last data for table with remain condition in there!
	 * @throws SQLException 
	 */
	private void createLastData() throws SQLException {
		// lastEndValidValue.entrySet()
		for (Map.Entry<String, CreateDataObj> e : saveCalcObj.entrySet()) {
			String lastVal = e.getValue().getLastEndValidValue();
			String fullTableColName = e.getKey();
			
			// Get tableName and colName
			String tableName = commonService.getTableAndColName(fullTableColName)[0];
			String colName = commonService.getTableAndColName(fullTableColName)[1];
			ColumnInfo colInfo = commonService.getColumnInfo(tableName, colName);
			
			// When last have calculator in mapping
			List<ColumnInfo> l = tableData.get(tableName);
			if (!lastVal.isEmpty()) {
				// New columnInfo
				ColumnInfo columnInfo = new ColumnInfo(colName, lastVal);
				
				columnInfo.setTypeName(colInfo.typeName);
				columnInfo.setTypeValue(colInfo.typeValue);
				
				l.add(columnInfo);
			} else {
				// Process create last data
				processGetLastData(l, colInfo, tableName, colName);
			}
		}
	}

	/**
	 * Get all mapping for each column With each column will find all related
	 * column. Put data to columnMap variable
	 */
	private Map<String, List<Cond>> getAllMappingColum(Map<String, Set<String>> columnMapping) {
		Map<String, List<Cond>> columnMap = new HashMap<>();
		for (Map.Entry<String, Set<String>> e : columnMapping.entrySet()) {
			Set<String[]> mappings = new HashSet<>();
			Map<String, String> parentMap = new HashMap<>();
			String curKey = e.getKey();
			String prevCol = curKey;
			Set<String> visited = new HashSet<>();
			for (String val : e.getValue()) {
				Queue<String> toExploder = new LinkedList<>();
				toExploder.add(val);
				visited.add(curKey);
				parentMap.put(val, prevCol);
				
				while (!toExploder.isEmpty()) {
					String cur = toExploder.remove();
					if (visited.contains(cur)) {
						continue;
					}
					visited.add(cur);	
					mappings.add(new String[] {cur, parentMap.get(cur)});
					if (columnMapping.containsKey(cur)) {
						for (String t : columnMapping.get(cur)) {
							if (!t.equals(curKey)) {
								parentMap.put(t, cur);
								toExploder.add(t);
							}
						}
					}
				}
			}

			// Need Set<Cond>
			// KEY ==> aliasTable.aliasColumn => alias
			// VALUE COND {operator, value{KEY}}
			List<Cond> s = new ArrayList<>();

			for (String[] c : mappings) {
				String[] operators = infoCol.get(c[0] + "-" + c[1]);
				if (operators == null) {
					continue;
				}
				if (saveCalcObj.containsKey(c[0])) {
					saveCalcObj.get(c[0]).getListOperator().add(operators[0]);
				} else {
					CreateDataObj createObj = new CreateDataObj();
					createObj.getListOperator().add(operators[0]);
					saveCalcObj.put(c[0], createObj);
				}
				
				if (saveCalcObj.containsKey(c[1])) {
					saveCalcObj.get(c[1]).getListOperator().add(operators[1]);
				} else {
					CreateDataObj createObj = new CreateDataObj();
					createObj.getListOperator().add(operators[1]);
					saveCalcObj.put(c[1], createObj);
				}
				
				boolean flgAdd = false;
				// Check have contains
				if (s.size() > 0) {
					for (Cond tmp : s) {
						if ((tmp.operator.equals(operators[0]) && tmp.value.equals(c[0]) && 
								tmp.operatorRight.equals(operators[1]) && tmp.rightValue.equals(c[1])) || 
								(tmp.operator.equals(operators[1]) && tmp.value.equals(c[1]) && 
										tmp.operatorRight.equals(operators[0]) && tmp.rightValue.equals(c[0]))) {
							flgAdd = false;
							break;
						} else {
							flgAdd = true;
						}
					}
				} else {
					flgAdd = true;
				}
				if (flgAdd) {
					Cond cond = new Cond(operators[0], c[0], operators[1], c[1]);
					s.add(cond);
				}
			}
			columnMap.put(e.getKey(), s);
		}
		return columnMap;
	}

	/**
	 * columnMap variable (mapping of column)
	 * validValuesForColumn valid values current of column
	 * @return push value to dataTable
	 * @throws SQLException 
	 */
	private void exeCalcForMappingKey(Map<String, List<Cond>> columnMap) throws SQLException {
		// table.colName => all column mapping of current table.colName
		// Map<String, Set<Cond>> columnMap;
		
		Set<String> visitedMapping = new HashSet<>();
		
		// table.colName => condition valid
		// Map<String, List<Cond>> columnMap;
		for (Map.Entry<String, List<Cond>> e : columnMap.entrySet()) {
			String col = e.getKey();
			String tableName = commonService.getTableAndColName(col)[0];
			String colName = commonService.getTableAndColName(col)[1];
			
			// calculated this column!
			if (visitedMapping.contains(col)) {
				continue;
			}
			
			// Get valid value of column
			List<Cond> validV = saveCalcObj.get(col).getValidValuesForColumn();
			String lastValidV = saveCalcObj.get(col).getLastEndValidValue();
			
			ColumnInfo t = commonService.getColumnInfo(tableName, colName);
			ColumnInfo colInfo = new ColumnInfo(t.getName(), "", t.getTypeName(), t.getTypeValue(),
						t.getIsNull(), t.getIsPrimarykey(), t.getIsForeignKey(), t.getUnique());

					
			String dataType = commonService.getDataTypeOfColumn(colInfo.getTypeName());
			
			int len = commonService.getLengthOfColumn(colInfo);
			
			List<String> validOfCol = new ArrayList<>();
			
			if (lastValidV != null && !lastValidV.isEmpty()) {
				validOfCol.add(lastValidV);
			} else {
				// When size = 1 => use equals(=)
				if (validV == null || validV.isEmpty()) {

					// When not validValue for this column => free style this case.
					// Maybe data type, min-len => push default key for this.
					processGenKey(tableName, validOfCol, validV, colInfo, dataType, len, colInfo.isKey());
				} else if (validV.size() == 1) {
					validOfCol.add(validV.get(0).value);
				} else if (validV.size() > 1) {
					processGenKey(tableName, validOfCol, validV, colInfo, dataType, len, colInfo.isKey());
				}
			}
			
			if (commonService.getCommonCreateObj().getType() == 0) {
				dataType = colInfo.getTypeName();
			}
			
			// Save in there!
			// Use DFS confirm this case!
			Stack<NodeColumn> toExploder = new Stack<>();
			Map<NodeColumn, NodeColumn> parentMap = new HashMap<>();
			
			boolean flgOut = false;
			// Init 
			// index -> Cond in e.getValue()
			// 0 -> Cond in e.getValue()
			// 0 -> Cond = value => tableName.colName, operator => <=, >=, >, <, !=
			HashMap<Integer, Cond> loopSearch = new HashMap<>(e.getValue().size());
			int i = 0;
			for (Cond conD : e.getValue()) {
				if ((saveCalcObj.get(conD.value) != null &&
						!saveCalcObj.get(conD.value).getLastEndValidValue().isEmpty())) {
					flgOut = true;
					break;
				}
				loopSearch.put(i, conD);
				++i;
			}
			
			if(flgOut) {
				continue;
			}
			
			// Init
			i = validOfCol.size() - 1;
			for (; i >= 0; --i) {
				NodeColumn nodeCol = new NodeColumn(col, validOfCol.get(i), 0, loopSearch.get(0), null);
				toExploder.add(nodeCol);
			}
			
			NodeColumn nodeGoal = processCalKeyMap(toExploder, parentMap, e.getValue(), validOfCol, 
					loopSearch, dataType);
			
			// Find valid value path
			// Next to insert to data table.
			// Flag true all Set<condtion> include this column! 
			// Will not execute again this link mapping.
			List<NodeColumn> pathValidValue = findPathValidForMapping(parentMap, nodeGoal);
			i = 0;
			for (; i < pathValidValue.size(); ++i) {
				NodeColumn cur = pathValidValue.get(i);
				
				// Mark color for column Info
				if (saveCalcObj.containsKey(cur.tableColumnName)) {
					System.out.println(cur.tableColumnName + " - " + pathValidValue.get(i).val);
					System.out.println();
					saveCalcObj.get(cur.tableColumnName).setLastEndValidValue(pathValidValue.get(i).val);
					saveCalcObj.get(cur.tableColumnName).setMarkColor("MARK_COLOR_" + idxColor);
				} else {
					CreateDataObj createObj = new CreateDataObj();
					createObj.setLastEndValidValue(pathValidValue.get(i).val);
					createObj.setMarkColor("MARK_COLOR_" + idxColor);
					saveCalcObj.put(cur.tableColumnName, createObj);
				}
				visitedMapping.add(cur.tableColumnName);
			}
			idxColor++;
		}
	}
	
	/**
	 * @param 1 value start with operator >=
	 * @param 2 value end operator <=
	 * @param 3 datatype of column
	 * @param len of value
	 * @return List key
	 */
	public List<String> genAutoKey(String valGreater, String valLess, String dataType, int len) {
		List<String> res = new ArrayList<>();
		
		boolean hasLess = !valLess.isEmpty();
		
		// Calculator increase or decrease?
		boolean isIncrease = false;
		if (valGreater.isEmpty() && valLess.isEmpty()) {
			isIncrease = true;
		} else if (!valGreater.isEmpty()) {
			isIncrease = true;
		}
		String curVal = isIncrease ? valGreater.isEmpty() ? genKeyWithLen(dataType, len) : valGreater : valLess;
		
		int limit = 10000;
		for (int i = 1; i <= limit; ++i) {
			res.add(curVal);
			String newVal = "";
			if (dataType.equals("date")) {
				newVal = genKeyWithTypeDate(isIncrease, curVal);
				if (hasLess && isIncrease) {
					Date t1 = parseStringToDate(valLess);
					Date t2 = parseStringToDate(newVal);
					
					// New value large than limit value
					if (t2.compareTo(t1) > 0) {
						break;
					}
				}
			} else if (dataType.equals("number")) {
				newVal = genKeyWithTypeNumber(isIncrease, curVal);
				
				Integer t2 = parseStringToInt(newVal);
				
				// When greater len stop!
				if (newVal.length() > len || t2 < 0) {
					break;
				}
				
				if (hasLess && isIncrease) {
					Integer t1 = parseStringToInt(valLess);
					
					// New value large than limit value
					if (t2 > t1) {
						break;
					}
				}
			} else if (dataType.equals("char")) {
				newVal = genKeyWithTypeChar(isIncrease, curVal);
				// When greater len stop!
				if (newVal.length() > len) {
					break;
				}
			}
			curVal = newVal;
		}
		return res;
	}
	
	/**
	 * Just confirm in normal character [26 character]
	 * @param type (++, --)
	 * @param curVal current value
	 * @return String new value after (++, --)
	 */
	public String genKeyWithTypeChar(boolean isIncrease, String curVal) {
		// Init character
		char[] chr = new char[26];
		for (int i = 0; i < 26; ++i) {
			chr[i] = (char) ('a' + i); 
		}
		
		if (curVal == null || curVal.isEmpty()) {
			curVal = "a";
		}
		
		char[] curChar = curVal.toCharArray();
		
		// Increase
		// 'z' -> 'aa'
		if (isIncrease) {
			int remain = 0;
			for (int i = curChar.length - 1; i >= 0; --i) {
				char c = curChar[i];
				if (c == 'z') {
					remain = 1;
					if (i == 0) {
						return repeat('a', curChar.length + 1);
					}
				} else {
					remain = 0;	
					curChar[i] = (char) (c + 1);
				}
				
				if (remain == 0) {
					return String.valueOf(curChar);
				}
			}
		}
		
		// Decrease
		// 'abcdf' -> 'abcde'
		int remain = 0;
		for (int i = curChar.length - 1; i >= 0; --i) {
			char c = curChar[i];
			if (c == 'a') {
				remain = 1;
				if (i == 0) {
					return repeat('z', curChar.length - 1);
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
	 * @param len need repeat
	 * @return String repeated
	 */
	private String repeat(char c, int len) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; ++i) {
			sb.append("" + c);
		}
		return sb.toString();
	}
	
	/**
	 * @param type (++, --)
	 * @param curVal current value
	 * @return String new value after (++, --)
	 */
	public String genKeyWithTypeDate(boolean isIncrease, String curVal) {
		Date curD;
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		try {
			curD = sdf.parse(removeSpecifyCharacter("'", curVal));
			c.setTime(curD);
		} catch(ParseException e) {
			e.printStackTrace();
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
	public String genKeyWithTypeNumber(boolean isIncrease, String curVal) {
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
			System.out.println("Format number error!");
		}
		
		return val;
	}
	
	/**
	 * 
	 * @param curValValid
	 * @param dataType
	 * @param conditionInWhere
	 * @param valInValid value in valid of this column
	 */
	public List<String> calculatorValidValWithColumnCondition(List<String> curValValid, String dataType,
			List<Cond> conditionInWhere, List<String> valInValid) {
		List<String> res = new ArrayList<>();
		
		// Check condition mapping
		for (int i = 0; i < conditionInWhere.size(); ++i) {
			String operator = conditionInWhere.get(i).operator;
			String value = removeSpecifyCharacter("'", conditionInWhere.get(i).value);
			
			for (int j = 0; j < curValValid.size(); ++j) {
				String curValue = removeSpecifyCharacter("'", curValValid.get(j));
				boolean flg = false;
				switch (operator) {
				case "=":
					if (value.equals(curValValid.get(j))) {
						flg = true;
					}
					break;
				case "<=":
					if (dataType.equals("number")) {
						Integer curI = Integer.parseInt(value);
						Integer innerI = parseStringToInt(curValue);
						if (innerI <= curI) {
							flg = true;
						}
					} else if (dataType.equals("date")) {
						Date curD = parseStringToDate(value);
						Date innerD = parseStringToDate(curValue);
						if (innerD.compareTo(curD) <= 0) {
							flg = true;
						}
					}
					break;
				case ">=":
					if (dataType.equals("number")) {
						Integer curI = Integer.parseInt(value);
						Integer innerI = parseStringToInt(curValue);
						if (innerI >= curI) {
							flg = true;
						}
					} else if (dataType.equals("date")) {
						Date curD = parseStringToDate(value);
						Date innerD = parseStringToDate(curValue);
						if (innerD.compareTo(curD) >= 0) {
							flg = true;
						}
					}
					break;
				default:
					System.out.println("Not other condition in there!");
					assert(false);
					break;
				}
				
				// Current value not in InValid value
				if (valInValid != null && valInValid.contains(curValue)) {
					flg = false;
				}
					
				if (flg) {
					res.add(curValue);
				}
			}
			
			curValValid = res;
			res = new ArrayList<>();
		}
		
		// Remove all invalid value
		if (valInValid != null && !valInValid.isEmpty()) {
			Queue<String> toExploder = new LinkedList<>();
			for (int i = 0; i < curValValid.size(); ++i) {
				toExploder.add(curValValid.get(i));
			}
			res.clear();
			while (!toExploder.isEmpty()) {
				String curVal = toExploder.poll();
				if (!valInValid.contains(curVal)) {
					res.add(curVal);
				}
			}
			curValValid = res;
		}
		return curValValid;
	}
	
	
	/**
	 * Parse string to date with format yyyy-MM-dd
	 * @return date formated
	 */
	public Date parseStringToDate(String origin) {
		Date curDate = new Date();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			curDate = sdf.parse(removeSpecifyCharacter("'", origin));
		} catch (ParseException e) {
			System.out.println("Parse string to date error!");
		}
		return curDate;
	}
	
	public Integer parseStringToInt(String origin) {
		Integer res = 0;
		try {
			res = Integer.parseInt(origin);
		} catch (NumberFormatException e) {
			System.out.println("Parse string to number error!");
			e.printStackTrace();
		}
		return res;
	}
	
	/**
	 * Find path valid in parentMap from node goal.
	 * @param parentMap
	 * @param nodeGoal
	 * @return
	 */
	public List<NodeColumn> findPathValidForMapping(Map<NodeColumn, NodeColumn> parentMap, 
			NodeColumn nodeGoal) {
		List<NodeColumn> res = new ArrayList<>();
		NodeColumn curNode = nodeGoal;
		while (curNode != null) {
			res.add(curNode);
			curNode = parentMap.get(curNode);
		}
		return res;
	}
	
	/**
	 * Genarate key with len
	 * @param len
	 * @return one value with this len
	 */
	private String genKeyWithLen(String dataType, int len) {
		if (dataType.equals("date")) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");  
			LocalDateTime now = LocalDateTime.now();  
			return dtf.format(now);  
		}
		
		StringBuilder res = new StringBuilder();
		if (dataType.equals("number")) {
			res.append("1");
		} else if (dataType.equals("char")) {
			res.append("a");
		}
		return res.toString();
	}
	
	/**
	 * Remove all specify character in string origin
	 * 
	 * @param specifyStr String of specify character to remove.
	 * @param origin
	 * @return String without character in specifyStr.
	 */
	private String removeSpecifyCharacter(String specifyStr, String origin) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < origin.length(); ++i) {
			if (!specifyStr.contains("" + origin.charAt(i))) {
				sb.append(origin.charAt(i));
			}
		}
		return sb.toString();
	}

	/**
	 * Check valid value
	 * @param List<Cond> validValueOfColumn
	 * @param value String current value
	 * @param dataType dataType of column
	 * @return return when val Valid in list<Cond>
	 */
	public boolean checkValidValue(List<Cond> validVal, String val, String dataType) {
		boolean flgCheck = true;
		val = removeSpecifyCharacter("'", val);
		for (Cond cond : validVal) {
			String innerVal = removeSpecifyCharacter("'", cond.value);
			switch (cond.operator) {
			case "=":
				if (!cond.value.equals(val)) {
					flgCheck = false;
				}
				break;
			case ">=":
				if (dataType.equals("number")) {
					Integer curI = parseStringToInt(val);
					Integer innerI = parseStringToInt(innerVal);
					if (curI < innerI) {
						flgCheck = false;
						break;
					}
				} else if (dataType.equals("date")) {
					Date curD = parseStringToDate(val);
					Date innerD = parseStringToDate(innerVal);
					if (curD.compareTo(innerD) < 0) {
						flgCheck = false;
						break;
					}
				}
				break;
			case "<=":
				if (dataType.equals("number")) {
					Integer curI = parseStringToInt(val);
					Integer innerI = parseStringToInt(innerVal);
					if (curI > innerI) {
						flgCheck = false;
						break;
					}
				} else if (dataType.equals("date")) {
					Date curD = parseStringToDate(val);
					Date innerD = parseStringToDate(innerVal);
					if (curD.compareTo(innerD) > 0) {
						flgCheck = false;
						break;
					}
				}
				break;
			default:
				System.out.println("Other operator?");
				break;
			}
		}
		return flgCheck;
	}
	
	
	/**
	 * Process gen key for (primary key || foreign key) or not
	 * @param tableName
	 * @param curValidVal
	 * @param validVal
	 * @param colInfo
	 * @param isKey
	 * @throws SQLException
	 */
	private void processGenKey(String tableName, List<String> curValidVal, List<Cond> validVal, ColumnInfo colInfo, 
			String dataType, int len, boolean isKey) throws SQLException {
		// When calculator not in mapping 
		// This case will read last condition remain -> gendata
		if (validVal == null || validVal.isEmpty()) {
			if (isKey) {
				curValidVal.addAll(dbServer.genListUniqueVal(tableName, colInfo, "", ""));
			} else {
				curValidVal.addAll(genAutoKey("", "", dataType, len));
			}
			return;
		} 

		boolean flgEqual = false;
		boolean flgGreater = false;
		boolean flgLess = false;
		
		String valLess = "";
		String valGreater = "";
		
		int cnt = 0;
		
		// Sort value valid
		Collections.sort(validVal, new Comparator<Cond>() {
			@Override
			public int compare(Cond c1, Cond c2) {
				String val1 = removeSpecifyCharacter("'", c1.value);
				String val2 = removeSpecifyCharacter("'", c2.value);
				
				// Priority equals!
				if (c1.operator.equals("=") && c2.operator.equals("=")) {
					return 0;
				} else if (c1.operator.equals("=")) {
					return 1;
				} else if (c2.operator.equals("=")) {
					return -1;
				}
				// Priority value sorted asc
				// When value equals => will sort desc of prirityOperator >= <=
				if (dataType.equals("number")) {
					Integer i1 = parseStringToInt(val1);
					Integer i2 = parseStringToInt(val2);
					if (Integer.compare(i1, i2) == 0) {
						Integer inner1 = priorityOfOperator.get(c1.operator);
						Integer inner2 = priorityOfOperator.get(c2.operator);
						return validVal.size() % 2 == 0 ? inner2 - inner1 : inner1 - inner2;
					}
					return Integer.compare(i1, i2);
				} else if (dataType.equals("date")) {
					Date cur1 = parseStringToDate(val1);
					Date cur2 = parseStringToDate(val2);
					if (cur1.compareTo(cur2) < 0) {
						return -1;
					} else if (cur1.compareTo(cur2) > 0) {
						return 1;
					} else {
						Integer i1 = priorityOfOperator.get(c1.operator);
						Integer i2 = priorityOfOperator.get(c2.operator);
						return validVal.size() % 2 == 0 ? i2 - i1 : i1 - i2;
					}
				}
				return 0;
			}
		});

		for (int i = 0; i < validVal.size(); ++i) {
			Cond cond = validVal.get(i);
			
			String operator = cond.operator;
			String val = removeSpecifyCharacter("'", cond.value);
			
			// Set data type for no-connection
			if (commonService.getCommonCreateObj().getType() == 0) {
				setDataTypeForNoConnection(colInfo, operator, val);
			}
			
			switch (operator) {
			case "=":
				flgEqual = true;
				curValidVal.add(val);
				break;
			case "<=":
				flgLess = true;
				if (valLess.isEmpty()) {
					valLess = val;
				}
				break;
			case ">=":
				flgGreater = true;
				if (valGreater.isEmpty()) {
					valGreater = val;
				}
				break;
			}
			
			if (!flgEqual && (flgLess || flgGreater) && curValidVal.isEmpty()) {
				cnt++;
				// Confirm pair
				// Gen value for pair
				if (cnt == 2) {
					
					if (operator.equals("<=")) {
						// Get previous operator
						Cond prevCond = validVal.get(i - 1);
						if (prevCond.operator.equals("<=")) {
							valLess = prevCond.value;
							cnt = 1;
							valGreater = "";
							flgLess = true;
							flgGreater = false;
						} else {
							// Valid
							if (isKey) {
								curValidVal.addAll(dbServer.genListUniqueVal(tableName, colInfo, valGreater, valLess));
							} else {
								curValidVal.addAll(genAutoKey(valGreater, valLess, dataType, len));
							}
							valLess = "";
							valGreater = "";
							cnt = 0;
							flgLess = false;
							flgGreater = false;
						}
					} else {
						// Get previous operator
						Cond prevCond = validVal.get(i - 1);
						if (prevCond.operator.equals("<=")) {
							if (isKey) {
								curValidVal.addAll(dbServer.genListUniqueVal(tableName, colInfo, valGreater, ""));
								curValidVal.addAll(dbServer.genListUniqueVal(tableName, colInfo, "", valLess));
							} else {
								curValidVal.addAll(genAutoKey(valGreater, "", dataType, len));
								curValidVal.addAll(genAutoKey("", valLess, dataType, len));
							}
							valLess = "";
							valGreater = "";
							cnt = 0;
							flgLess = false;
							flgGreater = false;
						} else {
							valLess = "";
							valGreater = val;
							cnt = 1;
							flgLess = false;
							flgGreater = true;
						}
					}
				}
			}
		}
		
		// Gen auto key
		// May be call method a Trung get primarykey
		// input(tableName.colName) => List<Primary key>
		if (!flgEqual && (flgLess || flgGreater) && curValidVal.isEmpty()) {
			if (isKey) {
				curValidVal.addAll(dbServer.genListUniqueVal(tableName, colInfo, valGreater, valLess));
			} else {
				curValidVal.addAll(genAutoKey(valGreater, valLess, dataType, len));
			}
		}
		
		// Del invalid
		String tableColName = tableName + "." + colInfo.getName();
		List<String> inValidVal = saveCalcObj.get(tableColName).getValueInValidOfColumn();
		int i = 0;
		while (i < curValidVal.size()) {
			if (curValidVal.size() <= 0) {
				break;
			}
			String cur = curValidVal.get(i);
			// Check not in
			if (inValidVal != null && inValidVal.contains(cur)){
				curValidVal.remove(i);
			} else {
				i++;
			}
		}
		
		if (curValidVal.size() <= 0) {
			throw new NotFoundValueSQLException("Not found valid value for this SQL");
		}
	}
	
	
	private Map<String, List<ColumnInfo>> processInsert(Map<String, List<List<ColumnInfo>>> clientData,
			int idxRow) throws Exception {
		Map<String, List<ColumnInfo>> res = new HashMap<>();
		Map<String, List<ColumnInfo>> tableInfo = commonService.getTableInfo();
		for (Map.Entry<String, List<ColumnInfo>> e : tableInfo.entrySet()) {
			String tableName = e.getKey();
			List<ColumnInfo> l = new ArrayList<>();
			Set<String> hasColumn = new HashSet<>();
			// Init
			for (ColumnInfo colInfo : e.getValue()) {
				if (!hasColumn.contains(colInfo.getName())) {
					l.add(new ColumnInfo(removeSpecifyCharacter("'", colInfo.getName()), "", colInfo.getTypeName(),
							colInfo.getTypeValue(), colInfo.getIsNull(), colInfo.getIsPrimarykey(),
							colInfo.getIsForeignKey(), colInfo.getUnique()));
					hasColumn.add(colInfo.getName());
				}
			}
			
			
			// Data da mapping can't change
			// Set again value
			List<ColumnInfo> data = tableData.get(tableName);
			
			// confirm KEY no value
			ColumnInfo colNoVal = null;
						
			if (data != null && !data.isEmpty()) {
				for (ColumnInfo colInfo : l) {
					for (ColumnInfo d : data) {
						if (colInfo.getName().equals(d.getName())) {
							colInfo.val = removeSpecifyCharacter("'", d.val);
							if (saveCalcObj.containsKey(tableName + "." + colInfo.getName()) &&
									!saveCalcObj.get(tableName + "." + colInfo.getName()).getMarkColor().isEmpty()) {
								colInfo.color = saveCalcObj.get(tableName + "." + colInfo.getName()).getMarkColor();
							} else {
								CreateDataObj createObj;
								if (saveCalcObj.containsKey(tableName + "." + colInfo.getName())) {
									createObj = saveCalcObj.get(tableName + "." + colInfo.getName());
								} else {
									createObj = new CreateDataObj();
								}
								colInfo.color = "MARK_COLOR_47";
								createObj.setMarkColor("MARK_COLOR_47");
							}
						}
					}
					
					if (colInfo.isKey() && colInfo.val.isEmpty()) {
						colNoVal = colInfo;
					}
				}
			} 
			
			if (colNoVal != null) {
				Map<String, ColumnInfo> mapVal = genValueForKeyNoCondition(tableName, colNoVal);
				for (ColumnInfo colInfo : l) {
					if (colInfo.isKey() && colInfo.val.isEmpty()) {
						colInfo.val = removeSpecifyCharacter("'", mapVal.get(tableName + "." + colInfo.getName()).val);
					}
				}
			}
			
			// get unique val
			for (ColumnInfo colInfo : l) {
				if (colInfo.unique && colInfo.getVal().isEmpty()) {
					colInfo.val = removeSpecifyCharacter("'", dbServer.genListUniqueVal(tableName, colInfo, "", "").get(0));
				}
			}
			
			String aliasName = aliasNameTable.get(tableName);
			commonService.addColumnGetFromSelect(l, tableName, aliasName);
			commonService.setClientData(l, clientData, tableName, idxRow);
			
			// Add default value
			for (ColumnInfo colInfo : l) {
				if (colInfo.getVal().isEmpty()) {
					String dataType = colInfo.getTypeName().equals("date") ? "date" : ""; 
					colInfo.val = fakerService.getDataByColumn(colInfo.getName(), dataType, this.locale);	
				}
			}
			
			// Check foreign key has exists
			for (ColumnInfo colInfo : l) {
				if (colInfo.getIsForeignKey()) {
					InforTableReferFK foreingKeyInfo = dbServer.checkInforFK(commonService.getCommonCreateObj().getSchema(), removeSpecifyCharacter("'", tableName), colInfo);
					if (!foreingKeyInfo.isHasExist()) {
						foreignKeyNotExistsInmainTable.put(foreingKeyInfo.getTableReferFKName(), foreingKeyInfo);
					}
				}
			}
			
			res.put(tableName, l);
		}
		return res;
	}
	
	/**
	 * Gen value for key no condition
	 * @param tableName
	 * @param colInfo
	 * @return Map<String, ColumnInfo> => colName = Key
	 * @throws SQLException
	 */
	private Map<String, ColumnInfo> genValueForKeyNoCondition(String tableName, ColumnInfo colInfo) throws SQLException {
		Map<String, ColumnInfo> res = new HashMap<>();
		
		// TODO
		// Error
		List<String> listVal = dbServer.genListUniqueVal(tableName, colInfo, "", "");
		
		// Gen value for key with no condition
		if (!commonService.isCompositeKey(tableName)) {
			res.put(tableName + "." + colInfo.name, new ColumnInfo(colInfo.name, listVal.get(0)));
		} else {
			for (String val : listVal) {
				Map<String, String> m = dbServer.genUniqueCol(SCHEMA_NAME, tableName, colInfo, val);
				if (m.size() > 0) {
					res.put(tableName + "." + colInfo.name, new ColumnInfo(colInfo.name, val));
					for (Map.Entry<String, String> e : m.entrySet()) {
						res.put(e.getKey(), new ColumnInfo(e.getKey(), e.getValue()));
					}
					break;
				}
			}
		}
		return res;
	}
	
	/**
	 * Process condition LIKE
	 * @param condition
	 * @return
	 */
	private String processConditionLike(String condition) {
		StringBuilder res = new StringBuilder();
		int n = condition.length();
		for (int i = 0; i < n; ++i) {
			if (operatorInLike.contains("" + condition.charAt(i))) {
				res.append("a");
			} else {
				res.append(condition.charAt(i));
			}
		}
		return res.toString();
	}

	/**
	 * Process add List Mark Color
	 * @return
	 */
	private List<String> processAddListMarkColor() {
		List<String> res = new ArrayList<>();
		saveCalcObj.entrySet().forEach(e -> {
			if (!e.getValue().getMarkColor().isEmpty() && !res.contains(e.getValue().getMarkColor())) {
				res.add(e.getValue().getMarkColor());
			}
		});
		Collections.sort(res);
		return res;
	}
	
	/**
	 * Process get last value
	 * @throws SQLException 
	 */
	private void processGetLastData(List<ColumnInfo> lstValue, ColumnInfo colInfo, String tableName, String colName) throws SQLException {
		List<Cond> validVal = saveCalcObj.get(tableName + "." + colName).getValidValuesForColumn();
		List<String> invalidVal = saveCalcObj.get(tableName + "." + colName).getValueInValidOfColumn();
		
		int len = commonService.getLengthOfColumn(colInfo);
		String dataType = commonService.getDataTypeOfColumn(colInfo.getTypeName());
		
		List<String> curValidVal = new ArrayList<>();

		// Manual gen value
		processGenKey(tableName, curValidVal, validVal, colInfo, dataType, len, colInfo.isKey());
		
		// Remove
		for (int i = 0; i < curValidVal.size(); ++i) {
			if (invalidVal == null || (!invalidVal.contains(curValidVal.get(i)))) {
				boolean flgAdd = false;
				// Key will gen value from DB
				if (commonService.isCompositeKey(tableName)) {
					Map<String, String> m = dbServer.genUniqueCol(SCHEMA_NAME, tableName, colInfo, curValidVal.get(i));
					if (m.size() == 0) {
						continue;
					}
					flgAdd = true;
					
					// Get composite key
					for (Map.Entry<String, String> entry : m.entrySet()) {
						ColumnInfo columnInfo = new ColumnInfo(entry.getKey(), entry.getValue());
						ColumnInfo colInner = commonService.getColumnInfo(tableName, commonService.getTableAndColName(entry.getKey())[1]);
						
						columnInfo.setTypeName(colInner.getTypeName());
						columnInfo.setTypeValue(colInner.getTypeValue());
					}
				} else {
					flgAdd = true;
				}
				
				if(flgAdd) {
					ColumnInfo columnInfo = new ColumnInfo(colName, curValidVal.get(i));
					
					// Set type for excute case add ' or not!
					columnInfo.setTypeName(colInfo.typeName);
					columnInfo.setTypeValue(colInfo.typeValue);
					
					lstValue.add(columnInfo);
					break;
				}
			}
		}
	}
	
	/**
	 * Process calculator for key mapping
	 * @throws SQLException 
	 */
	private NodeColumn processCalKeyMap(Stack<NodeColumn> toExploder, Map<NodeColumn, NodeColumn> parentMap,
			List<Cond> colMapping, List<String> validOfCol, Map<Integer, Cond> loopSearch, 
			String dataType) throws SQLException {
		NodeColumn nodeGoal = null;
		
		// Visited
		Set<NodeColumn> visited = new HashSet<>();
		
		// Init flagMeet
		boolean[] checkMeet = new boolean[colMapping.size()];
		
		for (int i = 0; i < colMapping.size(); ++i) {
			checkMeet[i] = false;
		}
		
		while (!toExploder.isEmpty()) {
			NodeColumn curNode = toExploder.pop();
			if (visited.contains(curNode)) {
				continue;
			}
			
			String val = curNode.val;
			int index = curNode.index;
			
			// Find goal then stop
			if (index == colMapping.size()) {
				nodeGoal = curNode;
				break;
			}
			
			// Get next mapping.
			Cond nextCond = loopSearch.get(index);
			
			// Find value of column
			NodeColumn tmp = curNode;
			while (tmp != null) {
				if (tmp.tableColumnName.equals(nextCond.rightValue) || 
						tmp.tableColumnName.equals(nextCond.value)) {
					val = tmp.val;
				}
				tmp = parentMap.get(tmp);
			}
			
			
			// Remove value generator
			// Just calculator first meet index
			// Valid value will increase
			if (!checkMeet[index]) {
				List<Cond> conditionInWhere = new ArrayList<>();
				// When has condition will remove current 
				if (saveCalcObj.containsKey(nextCond.value) && saveCalcObj.get(nextCond.value).getValidValuesForColumn().size() != 0) {
					if (!saveCalcObj.get(nextCond.value).getLastEndValidValue().isEmpty()) {
						conditionInWhere.add(new Cond("=", saveCalcObj.get(nextCond.value).getLastEndValidValue()));
					} else {
						conditionInWhere = saveCalcObj.get(nextCond.value).getValidValuesForColumn();
					}
					validOfCol = calculatorValidValWithColumnCondition(validOfCol, dataType,
							conditionInWhere, null);
				} 
				if (saveCalcObj.containsKey(nextCond.value)) {
					List<String> inValidValue = saveCalcObj.get(nextCond.value).getValueInValidOfColumn();
					validOfCol = calculatorValidValWithColumnCondition(validOfCol, dataType,
							conditionInWhere, inValidValue);
				}
//				if (validValuesForColumn.get(nextCond.rightValue) != null) {
//					if (lastEndValidValue.get(nextCond.rightValue) != null &&
//							!lastEndValidValue.get(nextCond.rightValue).isEmpty()) {
//						conditionInWhere.add(new Cond("=", lastEndValidValue.get(nextCond.rightValue)));
//					} else {
//						conditionInWhere = validValuesForColumn.get(nextCond.rightValue);
//					}
//					validOfCol = calculatorValidValWithColumnCondition(validOfCol, dataType,
//							conditionInWhere, null);
//				} 
//				
//				if (valueInValidOfColumn.containsKey(nextCond.rightValue)) {
//					List<String> inValidValue = valueInValidOfColumn.get(nextCond.rightValue);
//					validOfCol = calculatorValidValWithColumnCondition(validOfCol, dataType,
//							conditionInWhere, inValidValue);
//				}
				
				// Not found valid for mapping column
				if (validOfCol.isEmpty()) {
					throw new NotFoundValueSQLException("Not found valid value for this SQL!");
				}
			}
			
			checkMeet[index] = true;
			
			for (int i = validOfCol.size() - 1; i >= 0; --i) {
				
				String[] innerTableColName = commonService.getTableAndColName(nextCond.value);
				ColumnInfo t2 = commonService.getColumnInfo(innerTableColName[0], innerTableColName[1]);
				ColumnInfo colInnerInfo = new ColumnInfo(t2.getName(), "", 
						t2.getTypeName(), t2.getTypeValue(), t2.getIsNull(), 
						t2.getIsPrimarykey(), t2.getIsForeignKey(), t2.getUnique());
				
				boolean flgAdd = isKeyMapping(nextCond, val, validOfCol.get(i), dataType);

				Map<String, String> valCompositeKey = null; 
				if (flgAdd) {
					// Check value unique
					if ((commonService.getColumnInfo(innerTableColName[0], colInnerInfo.getName()).isKey()
							|| commonService.getColumnInfo(innerTableColName[0], colInnerInfo.getName()).getUnique()) &&
							!dbServer.isUniqueValue(innerTableColName[0], colInnerInfo, removeSpecifyCharacter("'", validOfCol.get(i)))) {
						flgAdd = false;
					} else {
						flgAdd = true;
					}
					boolean innerIsCompositeKey = commonService.isCompositeKey(innerTableColName[0]);
					
					// Execute for composite key
					if (flgAdd && innerIsCompositeKey) {
						valCompositeKey = dbServer.genUniqueCol(SCHEMA_NAME, innerTableColName[0], colInnerInfo, 
								removeSpecifyCharacter("'", validOfCol.get(i)));
						if (valCompositeKey.size() != 0) {
							flgAdd = true;
						} else {
							flgAdd = false;
						}
					}
				}
				
				if (flgAdd) {
					// Table columnName, value, index
					NodeColumn innerNode = new NodeColumn(nextCond.value.equals(curNode.tableColumnName) ? nextCond.rightValue : nextCond.value, 
							validOfCol.get(i), index + 1, nextCond, valCompositeKey);
					parentMap.put(innerNode, curNode);
					toExploder.add(innerNode);
				}
			}
		}
		return nodeGoal;
	}
	
	/**
	 * Check condition for mapping key
	 */
	private boolean isKeyMapping(Cond nextCond, String currentVal, String checkVal, String dataType) {
		boolean flgAdd = false;
		switch (nextCond.operator) {
		case "=":
			if (currentVal.equals(checkVal)) {
				flgAdd = true;
			}
			break;
		case "<=":
			if (dataType.equals("date")) {
				// date
				Date tmp1 = parseStringToDate(currentVal);
				Date tmp2 = parseStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) <= 0) {
					flgAdd = true;
				}
			} else if (dataType.equals("number")) {
				Integer int1 = parseStringToInt(currentVal);
				Integer int2 = parseStringToInt(checkVal);
				if (int2 <= int1) {
					flgAdd = true;
				}
			}
			// number
			break;
		case ">=":
			if (dataType.equals("date")) {
				// date
				Date tmp1 = parseStringToDate(currentVal);
				Date tmp2 = parseStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) >= 0) {
					flgAdd = true;
				}
			} else if (dataType.equals("number")) {
				Integer int1 = parseStringToInt(currentVal);
				Integer int2 = parseStringToInt(checkVal);
				if (int2 >= int1) {
					flgAdd = true;
				}
			}
			break;
		case "<":
			if (dataType.equals("date")) {
				// date
				Date tmp1 = parseStringToDate(currentVal);
				Date tmp2 = parseStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) < 0) {
					flgAdd = true;
				}
			} else if (dataType.equals("number")) {
				Integer int1 = parseStringToInt(currentVal);
				Integer int2 = parseStringToInt(checkVal);
				if (int2 < int1) {
					flgAdd = true;
				}
			}
			break;
		case ">":
			if (dataType.equals("date")) {
				// date
				Date tmp1 = parseStringToDate(currentVal);
				Date tmp2 = parseStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) > 0) {
					flgAdd = true;
				}
			} else if (dataType.equals("number")) {
				Integer int1 = parseStringToInt(currentVal);
				Integer int2 = parseStringToInt(checkVal);
				if (int2 > int1) {
					flgAdd = true;
				}
			}
			break;
		case "!=":
			if (!currentVal.equals(checkVal)) {
				flgAdd = true;
			}
			break;
		default:
			System.out.println("Not have other condition!");
			assert(false);
		}
		return flgAdd;
	}
	
	/**
	 * Process calculator for info col
	 * @param tableCol1
	 * @param tableCol2
	 * @param operator
	 */
	private void processCalInfoCol(String t1, String t2, String operator) {
		if (infoCol.containsKey(t1 + "-" + t2)) {
			String[] t = infoCol.get(t1 + "-" + t2);
			t[0] = operator; 
		} else {
			String[] t = new String[2];
			t[0] = operator;
			infoCol.put(t1 + "-" + t2, t);
		}
		
		if (infoCol.containsKey(t2 + "-" + t1)) {
			String[] t = infoCol.get(t2 + "-" + t1);
			t[1] = operator;
		} else {
			String[] t = new String[2];
			t[1] = operator;
			infoCol.put(t2 + "-" + t1, t);
		}
	}
	
	/**
	 * Check is Number
	 */
	private boolean isNumber(String origin) {
		try {
			Integer.parseInt(origin);
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
			sdf.parse(removeSpecifyCharacter("'", origin));
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	private String setDataTypeForNoConnection(ColumnInfo colInfo, String operator, String value) {
		String res = "";
		if (operator.equals("<=") || operator.equals(">=") || operator.equals("<") || operator.equals(">")) {
			if (isNumber(value)) {
				colInfo.setTypeName("number");
				colInfo.setTypeValue("6");
				res = "number";
			} else {
				// Date
				colInfo.setTypeName("date");
				colInfo.setTypeValue("");
				res = "date";
			}
		}
		return res;
	}
	
	/**
	 * Reformat data type for columnInfo which operator >=, <=, >, <
	 * @param colMapping
	 */
	private void exeReFormatDataType(Map<String, List<Cond>> columnMap) {
		saveCalcObj.entrySet().forEach(e -> {
			String[] tableColName = commonService.getTableAndColName(e.getKey());
			ColumnInfo colInfo = commonService.getColumnInfo(tableColName[0], tableColName[1]);
			List<String> listValue = e.getValue().getListValue();
				
			if (listValue.size() != 0) {
				for (String val : listValue) {
					if (val != null && !val.isEmpty()) {
						String dataType = "";
						String typeValue = "";
						if (isNumber(val)) {
							dataType = "number";
							typeValue = "6";
							colInfo.setTypeName(dataType);
							colInfo.setTypeValue(typeValue);
						} else if (isDate(val)) {
							// Date
							dataType = "date";
							colInfo.setTypeName(dataType);
						} else {
							// varchar
						}
					}
				}
			}
			
			// Update data for column mapping
			List<Cond> c = columnMap.get(tableColName[0] + "." + tableColName[1]);
			if (c != null) {
				for (Cond t : c) {
					String[] tableCol1 = commonService.getTableAndColName(t.value);
					String[] tableCol2 = commonService.getTableAndColName(t.rightValue);
					ColumnInfo col1 = commonService.getColumnInfo(tableCol1[0], tableCol1[1]);
					ColumnInfo col2 = commonService.getColumnInfo(tableCol2[0], tableCol2[1]);
					col1.setTypeName("number");
					col1.setTypeValue("6");
					col2.setTypeName("number");
					col2.setTypeValue("6");
				}
			}
		});
	}
	
	/**
	 * Add value for main table when foreign key not exists
	 * @param reponseData
	 */
	private void processWithForeignKey(Map<String, List<List<ColumnInfo>>> reponseData) {
		foreignKeyNotExistsInmainTable.entrySet().forEach(e -> {
			String tableRefer = e.getKey();
			List<ColumnInfo> columnsRefer = e.getValue().getColumnInfoLst();
			if (reponseData.containsKey(tableRefer)) {
				Map<String, String> mapVal = new HashMap<>();
				for (ColumnInfo colInfo : columnsRefer) {
					ColumnInfo currentInfo = commonService.getColumnInfo(tableRefer, colInfo.getName());
					if (currentInfo.getIsPrimarykey()) {
						mapVal.put(colInfo.getName(), colInfo.getVal());
					}
				}
				List<List<ColumnInfo>> t = reponseData.get(tableRefer);
				boolean hasExists = true;
				for (List<ColumnInfo> colsInfo : t) {
					int cnt = 0;
					for (ColumnInfo innerInfo : colsInfo) {
						if (innerInfo.getIsPrimarykey() && mapVal.get(innerInfo.getName()).equals(innerInfo.getVal())) {
							cnt++;
						}
					}
					if (cnt == mapVal.size()) {
						hasExists = false;
					}
				}
				if (hasExists) {
					t.add(columnsRefer);
				}
			} else {
				List<List<ColumnInfo>> t = new ArrayList<>();
				t.add(columnsRefer);
				reponseData.put(tableRefer, t);
			}
		});
	}
}
