package sql.generator.hackathon.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sql.generator.hackathon.create.CreateData;
import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.InfoDisplayScreen;
import sql.generator.hackathon.model.InforTableReferFK;
import sql.generator.hackathon.model.ObjForeignKeyInfo;

@Service
public class ExecuteDBSQLServer {

	Map<String, String> driverDB;
	Map<String, String> jdbcDB;
	
	public Connection connect;
	
	@Autowired
	BeanFactory beanFactory;
	
	public ExecuteDBSQLServer() {
		super();
		driverDB = new HashMap<String, String>();
		driverDB.put("Mysql", "com.mysql.cj.jdbc.Driver");
		driverDB.put("H2", "org.h2.Driver");
		driverDB.put("Oracle", "oracle.jdbc.OracleDriver");
		// url h2 jdbc:h2:~/test
		jdbcDB = new HashMap<String, String>();
		jdbcDB.put("Mysql", "jdbc:mysql://");
		jdbcDB.put("H2", "jdbc:h2:mem:testdb");
		jdbcDB.put("Oracle", "jdbc:oracle:thin:@");
	}
	
	//connect database
	public boolean connectDB(String tableSelected, String url, String schemaName, String user, String pass) throws Exception {
		
		DataSource dataSource = (DataSource)beanFactory.getBean("dataSource", driverDB.get(tableSelected), jdbcDB.get(tableSelected) + url + "/" + schemaName, user, pass);
		try {
			connect = dataSource.getConnection();
		} catch (Exception e) {
			return false;
		}
    	return true;
	}
	
	//disconnect database
	public void disconnectDB() throws SQLException {
		if (connect != null) {
			connect.close();
		}
	}
	
	//------------------------------------------------------------------------------
	// get infor table (columnName, isNull, dataType, PK, FK, unique, maxLength)
	public Map<String, List<ColumnInfo>> getInforTable(String schemaName, List<String> lstTableName) throws Exception {
		Map<String, List<ColumnInfo>> inforTable = new HashMap<String, List<ColumnInfo>>();
		List<ColumnInfo> list_col;
		ColumnInfo columnInfo;
		PreparedStatement p;
		for (String tableName : lstTableName) {
			list_col = new ArrayList<ColumnInfo>();
			p = connect.prepareStatement("SELECT COLUMN_NAME, COLUMN_KEY, IS_NULLABLE, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH\r\n" + 
					"FROM    \r\n" + 
					"    information_schema.columns c\r\n" + 
					"WHERE\r\n" + 
					"	TABLE_NAME = ? AND TABLE_SCHEMA = ?\r\n"
					+ "\n" +
					"order by ORDINAL_POSITION");
			p.setString(1, tableName);
			p.setString(2, schemaName);
	        ResultSet resultSet = p.executeQuery();
        	while (resultSet.next()) {
	        	columnInfo = new ColumnInfo();
				columnInfo.setName(resultSet.getString("COLUMN_NAME"));
				columnInfo.setTypeName(resultSet.getString("DATA_TYPE"));
				columnInfo.setTypeValue(resultSet.getString("CHARACTER_MAXIMUM_LENGTH"));
				
				if(resultSet.getString("IS_NULLABLE").equals("YES")) {
					columnInfo.setIsNull(true);
				}
				
				if(resultSet.getString("COLUMN_KEY").equals("PRI")){
					columnInfo.setIsPrimarykey(true);
				} else if (resultSet.getString("COLUMN_KEY").equals("UNI")) {
					columnInfo.setUnique(true);
				}
				if(isColForeignKey(schemaName, tableName, resultSet.getString("COLUMN_NAME"))) {
					columnInfo.setIsForeignKey(true);
				}
				list_col.add(columnInfo);
			}
	        
	        inforTable.put(tableName, list_col);
	        resultSet.close();
		}
        return inforTable;
	}
	
	//------------------------------------------------------------------------------
	// get infor table (columnName, isNull, dataType, PK, FK, unique, maxLength)
	public Map<String, List<ColumnInfo>> getInforTable(String schemaName, List<String> lstTableName,
			Map<String, List<String>> mappingAliasName) throws Exception {
		Map<String, List<ColumnInfo>> inforTable = new HashMap<String, List<ColumnInfo>>();
		List<ColumnInfo> list_col;
		ColumnInfo columnInfo;
		PreparedStatement p;
		for (String tableName : lstTableName) {
			list_col = new ArrayList<ColumnInfo>();
			p = connect.prepareStatement("SELECT COLUMN_NAME, COLUMN_KEY, IS_NULLABLE, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH\r\n" + 
					"FROM    \r\n" + 
					"    information_schema.columns c\r\n" + 
					"WHERE\r\n" + 
					"	TABLE_NAME = ? AND TABLE_SCHEMA = ?\r\n"
					+ "\n" +
					"order by ORDINAL_POSITION");
			p.setString(1, tableName);
			p.setString(2, schemaName);
	        ResultSet resultSet = p.executeQuery();
	        List<String> listAliasName = mappingAliasName.get(tableName);
	        for (String aliasName : listAliasName) {
	        	while (resultSet.next()) {
		        	columnInfo = new ColumnInfo();
					columnInfo.setName(resultSet.getString("COLUMN_NAME"));
					columnInfo.setTypeName(resultSet.getString("DATA_TYPE"));
					columnInfo.setTypeValue(resultSet.getString("CHARACTER_MAXIMUM_LENGTH"));
					
					if(resultSet.getString("IS_NULLABLE").equals("YES")) {
						columnInfo.setIsNull(true);
					}
					
					if(resultSet.getString("COLUMN_KEY").equals("PRI")){
						columnInfo.setIsPrimarykey(true);
					} else if (resultSet.getString("COLUMN_KEY").equals("UNI")) {
						columnInfo.setUnique(true);
					}
					if(isColForeignKey(schemaName, tableName, resultSet.getString("COLUMN_NAME"))) {
						columnInfo.setIsForeignKey(true);
					}
					columnInfo.setTableAlias(aliasName);
					list_col.add(columnInfo);
				}
	        }
	        
	        inforTable.put(tableName, list_col);
	        resultSet.close();
		}
        return inforTable;
	}
	
	private boolean isColForeignKey(String schemaName, String tableName, String colName) throws SQLException {
		PreparedStatement ps = connect.prepareStatement("SELECT\r\n" + 
				"    COUNT(*)\r\n" +
				"FROM\r\n" + 
				"    INFORMATION_SCHEMA.KEY_COLUMN_USAGE\r\n" + 
				"WHERE\r\n" + 
				"	REFERENCED_TABLE_SCHEMA = ?\r\n" + 
				"    AND TABLE_NAME = ?\r\n" + 
				"    AND COLUMN_NAME = ?");
		ps.setString(1, schemaName);
		ps.setString(2, tableName);
		ps.setString(3, colName);
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			if(rs.getInt(1) > 0) {
				return true;
			}
		}
		rs.close();
		return false;
	}
	
	//------------------------------------------------------------------------------
	// get object (column, data) to show screen
	public List<InfoDisplayScreen> getDataDisplay(String schemaName, List<String> tableNameLst) throws Exception {
		List<InfoDisplayScreen> infoDisplayScreenLst = new ArrayList<InfoDisplayScreen>();
		InfoDisplayScreen infoDisplayScreen;
		for (String tableName : tableNameLst) {
			infoDisplayScreen = new InfoDisplayScreen();
			infoDisplayScreen.setTableName(tableName);
			infoDisplayScreen.setListColumnName(this.getListColumn(schemaName, tableName));
			infoDisplayScreen.setListData(this.getListData(tableName));
			infoDisplayScreenLst.add(infoDisplayScreen);
		}
		return infoDisplayScreenLst;
	}

	//get list column to show screen
	private List<String> getListColumn(String schemaName, String tableName) throws Exception {
		List<String> list_col = new ArrayList<String>();
		PreparedStatement p = connect.prepareStatement("SELECT COLUMN_NAME FROM information_schema.columns WHERE TABLE_NAME = ? AND TABLE_SCHEMA = ? order by ORDINAL_POSITION");
		p.setString(1, tableName);
		p.setString(2, schemaName);
		ResultSet resultSet = p.executeQuery();
		
		while (resultSet.next()) {
			for (int i = 0; i< resultSet.getMetaData().getColumnCount(); i++) {
				list_col.add(resultSet.getString(i + 1));
			}
		}
		resultSet.close();
		return list_col;
	}
	
	//get list data to show screen, limit 20 record
	private List<List<String>> getListData(String tableName) throws Exception {
		List<List<String>> listData = new ArrayList<List<String>>();
		List<String> rowData;
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT * FROM ");
		SQL.append(tableName);
		SQL.append(" LIMIT 20");
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		
		while (resultSet.next()) {
			rowData = new ArrayList<String>();
			for (int i = 0; i< resultSet.getMetaData().getColumnCount(); i++) {
				if(i == 0) {
					rowData.add("false");
				}
				rowData.add(resultSet.getString(i + 1));
			}
			listData.add(rowData);
		}
		resultSet.close();
		return listData;
	}

	//------------------------------------------------------------------------------
	// check value exist in table or not
	public boolean isUniqueValue(String tableName, ColumnInfo columnInfo, String value) throws SQLException {
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT Count(*) FROM " + tableName);
		SQL.append(" WHERE " + columnInfo.getName() + " = '" + value +"'");
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		while (resultSet.next()) {
			if(("0").equals(resultSet.getString(1))) {
				return true;
			}
		}
		resultSet.close();
		return false;
	}
	
	//------------------------------------------------------------------------------
	// gene list value unique
	public List<String> genListUniqueVal(String tableName, ColumnInfo columnInfo, String start, String end) throws SQLException {
		List<String> lstUniqueVal = new ArrayList<String>();
		switch(columnInfo.getTypeName()) {
			case "varchar":
				lstUniqueVal = genListStringUnique(tableName, columnInfo);
				break;
			case "bigint":
			case "int":
				lstUniqueVal = genListNumberUnique(tableName, columnInfo, start, end);
				break;
			default:
				break;
		}
		
		return lstUniqueVal;
	}
	
	public List<String> getListUniqueVal(String tableName, ColumnInfo columnInfo) throws SQLException {
		List<String> res = new ArrayList<>();
		String sql = "select " + columnInfo.getName() + " from " + tableName;
		Statement stmt = connect.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next()) {
			String val = "";
			switch (columnInfo.getTypeName()) {
			case "bigint":
			case "int":
				val = String.valueOf(rs.getInt(columnInfo.getName()));
				break;
			case "varchar":
			case "nvarchar":
			case "nchar":
			case "char":
				val = rs.getString(columnInfo.getName());
				break;
			}
			res.add(val);
		}
		return res;
	}
	
	private List<String> genListStringUnique(String tableName, ColumnInfo columnInfo) throws SQLException {
		CreateData createData = new CreateData();
		List<String> lstStringUnique = new ArrayList<String>();
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT MAX(" + columnInfo.getName() + ") FROM " + tableName);
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		while (resultSet.next()) {
			String increaseValue = resultSet.getString(1);
			for (int i = 0; i < 10000; i++) {
				increaseValue = createData.genKeyWithTypeChar(true, increaseValue);
				lstStringUnique.add(increaseValue);
			}
		}
		resultSet.close();
		return lstStringUnique;
	}
	
	private List<String> genListNumberUnique(String tableName, ColumnInfo columnInfo, String start, String end) throws SQLException {
		List<String> lstNumberUnique = new ArrayList<String>();
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		int indexStart = 0;
		int indexEnd = 0;
		SQL.append("SELECT " + columnInfo.getName() + " FROM " + tableName);
		SQL.append(" WHERE ");
		if (!(start == null || start.isEmpty()) && !(end == null || end.isEmpty())) {
			indexStart = Integer.parseInt(start);
			indexEnd = Integer.parseInt(end);
		} else if ((start == null || start.isEmpty()) && (end == null || end.isEmpty())){
			indexStart = getNumberMax(tableName, columnInfo);
			indexEnd = indexStart + 10000;
		} else if(start == null || start.isEmpty()) {
			indexStart = Integer.parseInt(end) - 10000;
			indexEnd = Integer.parseInt(end);
		} else {
			indexStart = Integer.parseInt(start);
			indexEnd = Integer.parseInt(start)  + 10000;
		}
		SQL.append(columnInfo.getName() + " BETWEEN " + indexStart + " AND " + indexEnd);
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		
		for (int i = indexStart; i <= indexEnd; i++) {
			lstNumberUnique.add(Integer.toString(i));
		}
		while (resultSet.next()) {
			lstNumberUnique.remove(resultSet.getString(1));
		}
		resultSet.close();
		return lstNumberUnique;
	}
	

	
	// get number max in data
	private int getNumberMax(String tableName, ColumnInfo columnInfo) throws SQLException {
		int max = 0;
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT MAX(" + columnInfo.getName() + ") FROM " + tableName);
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		while (resultSet.next()) {
			max = resultSet.getInt(1);
		}
		resultSet.close();
		return max;
	}
	
	//------------------------------------------------------------------------------
	// gene value for primary key or unique 
	public Map<String, String> genUniqueCol(String schema, String tableName, ColumnInfo columnInfo, String value) throws SQLException {
		Map<String, String> mapUnique = new HashMap<String, String>();

		// get infor FOREIGN KEY
		List<ObjForeignKeyInfo> lstObjForeignKeyInfo = getInforColForeignKey(schema, tableName);
		
		String valForeignKey;
		// get value FOREIGN KEY
		for (ObjForeignKeyInfo objForeignKeyInfo : lstObjForeignKeyInfo) {
			valForeignKey = getValueForeignKey(tableName, objForeignKeyInfo);
			mapUnique.put(tableName + "." + objForeignKeyInfo.getColumnName(), valForeignKey);
		}
		
		// get column primary key
		List<ColumnInfo> listColPri = getColPrimaryKey(schema, tableName, columnInfo);
		
		// no PRI, UNI and FOREIGN
		if(listColPri.size() == 0) {
			return mapUnique;
		}
		
		// Exist PRI or UNI
		Map<String, ColumnInfo> mapValueKey = getValuePrimaryKey(tableName, listColPri, columnInfo);
		String randomValue = "";
		for (Map.Entry<String, ColumnInfo> entry : mapValueKey.entrySet()) {
			do {
				randomValue = createValueRandom(entry.getValue());
			} while (!isUniqueValue(tableName, entry.getValue(), randomValue));
			mapUnique.put(tableName + "." + entry.getKey(), randomValue);
		}
		return mapUnique;
	}
	
	//get column primary key and unique in table
	private List<ColumnInfo> getColPrimaryKey(String schema, String tableName, ColumnInfo columnInfo) throws SQLException {
		List<ColumnInfo> listColPri = new ArrayList<ColumnInfo>();
		ColumnInfo columnKeyReturn;
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE table_schema = '" + schema + "'");
		SQL.append(" AND (column_key = 'PRI' OR column_key = 'UNI') AND table_name = '" + tableName + "' AND COLUMN_NAME <> '" + columnInfo.getName() + "'");
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		while (resultSet.next()) {
			columnKeyReturn = new ColumnInfo();
			columnKeyReturn.setName(resultSet.getString(1));
			columnKeyReturn.setTypeName(resultSet.getString(2));
			listColPri.add(columnKeyReturn);
		}
		resultSet.close();
		return listColPri;
	}
	
	// get value of all column primary key
	private Map<String, ColumnInfo> getValuePrimaryKey(String tableName, List<ColumnInfo> listColKey, ColumnInfo columnInfo) throws SQLException {
		Map<String, ColumnInfo> mapValuePrimaryKey = new HashMap<String, ColumnInfo>();
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT ");
		for (int i = 0; i < listColKey.size(); i++) {
			if(i > 0) {
				SQL.append(", ");
			}
			SQL.append("MAX(" + listColKey.get(i).getName() + ")");
		}
		SQL.append(" FROM " + tableName + " LIMIT 1");
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		while (resultSet.next()) {
			for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
				listColKey.get(i).setVal(resultSet.getString(i + 1));
				mapValuePrimaryKey.put(listColKey.get(i).getName(), listColKey.get(i));
			}
		}
		resultSet.close();
		return mapValuePrimaryKey;
	}
	
	// get column foreign key
	private List<ObjForeignKeyInfo> getInforColForeignKey(String schema, String tableName) throws SQLException {
		List<ObjForeignKeyInfo> lstObjForeignKeyInfo = new ArrayList<ObjForeignKeyInfo>();
		ObjForeignKeyInfo objForeignKeyInfo;
		PreparedStatement p = connect.prepareStatement("SELECT\r\n" + 
				"   TABLE_NAME,\r\n" + 
				"	COLUMN_NAME,\r\n" + 
				"	REFERENCED_TABLE_NAME,\r\n" + 
				"	REFERENCED_COLUMN_NAME\r\n" + 
				"FROM\r\n" + 
				"    INFORMATION_SCHEMA.KEY_COLUMN_USAGE\r\n" + 
				"WHERE\r\n" + 
				"	 REFERENCED_TABLE_SCHEMA = ?\r\n" + 
				"    AND TABLE_NAME = ?");
		p.setString(1, schema);
		p.setString(2, tableName);
		ResultSet resultSet = p.executeQuery();
		while (resultSet.next()) {
			objForeignKeyInfo = new ObjForeignKeyInfo();
			objForeignKeyInfo.setTableName(resultSet.getString(1));
			objForeignKeyInfo.setColumnName(resultSet.getString(2));
			objForeignKeyInfo.setReferencedTableName(resultSet.getString(3));
			objForeignKeyInfo.setReferencedColumnName(resultSet.getString(4));
			lstObjForeignKeyInfo.add(objForeignKeyInfo);
		}
		resultSet.close();
		return lstObjForeignKeyInfo;
	}
	
	// get value foreign key
	private String getValueForeignKey(String tableName, ObjForeignKeyInfo lstObjForeignKeyInfo) throws SQLException {
		String valForeignKey = "";
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT " + lstObjForeignKeyInfo.getReferencedColumnName());
		SQL.append(" FROM " + lstObjForeignKeyInfo.getReferencedTableName() + " LIMIT 1");
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		while (resultSet.next()) {
			valForeignKey = resultSet.getString(1);
		}
		resultSet.close();
		return valForeignKey;
	}
	
	//create random String
	private String createValueRandom(ColumnInfo columnInfo) throws SQLException {
		int length = 0;
		int random = 0;
		String result = "";
		if(!(columnInfo.getVal() == null || columnInfo.getVal().isEmpty())) {
			length = columnInfo.getVal().length();
		}else {
			length = Integer.parseInt(columnInfo.getTypeValue());
		}
		switch (columnInfo.getTypeName()) {
			case "varchar":
				char[] ch = new char[length];
				// random char from 65 -> 122
				for (int i = 0; i < length; i++) {
					// random A -> Z
					random = new Random().nextInt(25) + 97;
					ch[i] = (char) (random);
				}
				result = String.valueOf(ch);
				break;
			case "date":
				result = String.valueOf(randomDate());
				break;
			case "int":
			case "bigint":
				result = String.valueOf(Integer.parseInt(columnInfo.getVal()) + 1);
				break;
			default:
		}
		return result;
	}
	
	// get Date random
	private String randomDate() {
		Random random = new Random();
		int minDay = (int) LocalDate.of(1900, 1, 1).toEpochDay();
		int maxDay = (int) LocalDate.of(2015, 1, 1).toEpochDay();
		long randomDay = minDay + random.nextInt(maxDay - minDay);

		LocalDate randomBirthDate = LocalDate.ofEpochDay(randomDay);
		return String.valueOf(randomBirthDate);
	}
	
	//------------------------------------------------------------------------------
	//check value FK is unique, if no return infor table refer of FK (table, column, value)
	public InforTableReferFK checkInforFK(String schema, String tableName, ColumnInfo columnInfo) throws Exception {
		InforTableReferFK inforTableReferFK = new InforTableReferFK();
		//check value exist or not yet
		if(isUniqueValue(tableName, columnInfo, columnInfo.getVal())) {
			inforTableReferFK.setHasExist(false);
			
			// get infor FOREIGN KEY
			ObjForeignKeyInfo objForeignKeyInfo = getTableReferFK(schema, tableName, columnInfo);
			inforTableReferFK.setTableReferFKName(objForeignKeyInfo.getReferencedTableName());
			
			// convert tableName -> List to do param for getInforTable()
			List<String> tableNameLst = Arrays.asList(objForeignKeyInfo.getReferencedTableName());
			
			// get column of table refer
			Map<String, List<ColumnInfo>> infoTableRefer = getInforTable(schema, tableNameLst);
			List<ColumnInfo> colTableReferList = infoTableRefer.get(objForeignKeyInfo.getReferencedTableName());
			List<String> rowData = getValueTableReferFK(objForeignKeyInfo.getReferencedTableName());
			List<ColumnInfo> columnInfoLst = new ArrayList<ColumnInfo>();
			for (int i = 0; i < colTableReferList.size(); i++) {
				if(colTableReferList.get(i).getName().equals(objForeignKeyInfo.getReferencedColumnName())) {
					// add value FK for column refer
					colTableReferList.get(i).color = columnInfo.color;
					colTableReferList.get(i).setVal(columnInfo.getVal());
				}else {
					// add value for other column (diff FK)
					colTableReferList.get(i).setVal(rowData.get(i));
				}
				columnInfoLst.add(colTableReferList.get(i));
			}
			inforTableReferFK.setColumnInfoLst(columnInfoLst);
			return inforTableReferFK;
		}
		inforTableReferFK.setHasExist(true);
		return inforTableReferFK;
	}
	
	private ObjForeignKeyInfo getTableReferFK(String schema, String tableName, ColumnInfo columnInfo) throws SQLException {
		ObjForeignKeyInfo objForeignKeyInfo = new ObjForeignKeyInfo();
		PreparedStatement p = connect.prepareStatement("SELECT\r\n" + 
//				"   TABLE_NAME,\r\n" + 
//				"	COLUMN_NAME,\r\n" + 
				"	REFERENCED_TABLE_NAME,\r\n" + 
				"	REFERENCED_COLUMN_NAME\r\n" + 
				"FROM\r\n" + 
				"    INFORMATION_SCHEMA.KEY_COLUMN_USAGE\r\n" + 
				"WHERE\r\n" + 
				"	 REFERENCED_TABLE_SCHEMA = ?\r\n" + 
				"    AND TABLE_NAME = ?\r\n" +
				"    AND COLUMN_NAME = ?");
		p.setString(1, schema);
		p.setString(2, tableName);
		p.setString(3, columnInfo.getName());
		ResultSet resultSet = p.executeQuery();
		while (resultSet.next()) {
//			objForeignKeyInfo.setTableName(resultSet.getString(1));
//			objForeignKeyInfo.setColumnName(resultSet.getString(2));
			objForeignKeyInfo.setReferencedTableName(resultSet.getString(1));
			objForeignKeyInfo.setReferencedColumnName(resultSet.getString(2));
		}
		resultSet.close();
		return objForeignKeyInfo;
	}
	
	private List<String> getValueTableReferFK(String tableReferFKName) throws SQLException {
//		List<List<String>> listData = new ArrayList<List<String>>();
		List<String> rowData = new ArrayList<String>();
		Statement stmt = connect.createStatement();
		StringBuilder SQL = new StringBuilder();
		SQL.append("SELECT * FROM ");
		SQL.append(tableReferFKName);
		SQL.append(" LIMIT 1");
		ResultSet resultSet = stmt.executeQuery(SQL.toString());
		
		while (resultSet.next()) {
//			rowData = new ArrayList<String>();
			for (int i = 0; i< resultSet.getMetaData().getColumnCount(); i++) {
				rowData.add(resultSet.getString(i + 1));
			}
		}
		resultSet.close();
		return rowData;
	}
}
