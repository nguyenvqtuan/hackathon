package sql.generator.hackathon.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import sql.generator.hackathon.model.ColumnInfo;

@Service
public class CreateService {
	
	private Map<String, List<ColumnInfo>> tableInfo = new HashMap<>();
	
	private ExecuteDBSQLServer executeDBServer;
	private Connection conn;
	
	public CreateService() {
	}
	
	public void init(ExecuteDBSQLServer executeDBServer) {
		this.conn = executeDBServer.connect;
		this.executeDBServer = executeDBServer;
	}
	
	public Map<String, List<ColumnInfo>> getTableInfo(String schema, List<String> listTable) throws Exception {
		tableInfo = executeDBServer.getInforTable(schema, listTable);
		return tableInfo;
	}
	
	/**
	 * Insert data table in DB
	 * @param tableName
	 * @param columnInfos
	 * @throws SQLException 
	 */
	public void insert(String tableName, List<ColumnInfo> columnInfos) throws SQLException {
		// Get all column
		// Get all value sorted follumn column
		Map<String, String> m = getMapColumnsAndValues(tableName, columnInfos);
		
		String sqlInsert = "INSERT INTO " + tableName + "(" + m.get("columns") + ") VALUES (" + m.get("values") + ")";
        try {
            // crate statement to insert student
            PreparedStatement stmt = conn.prepareStatement(sqlInsert);
            int c = stmt.executeUpdate();
            if (c == 0) {
            	System.out.println("Insert Error!");
            } else {
            	System.out.println("Insert success!");
            }
            
            stmt.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
        	conn.close();
        }
	}
	
	/**
	 * Get all colName
	 * @param ColumnInfos List columnInfo
	 * @return Map<String, String>all columnName
	 */
	public Map<String, String> getMapColumnsAndValues(String tableName, List<ColumnInfo> columnInfos) {
		
		// Init
		Map<String, String> res = new HashMap<>();
		StringBuilder colNames = new StringBuilder();
		StringBuilder values = new StringBuilder();
		
		for (ColumnInfo columnInfo : columnInfos) {
			if (colNames.length() != 0) {
				colNames.append(",");
			}
			
			if (values.length() != 0) {
				values.append(",");
			}
			
			colNames.append(columnInfo.name);
			
			// Get default value
			values.append(getCorrectValue(columnInfo.getTypeName(), columnInfo.getVal()));
		}
		
		
		res.put("columns", colNames.toString());
		res.put("values", values.toString());
		return res;
	}
	
	/**
	 * Get str column update
	 * @param columnInfos
	 * @return String column update (col1 = '5', col2 = '6')
	 */
//	public String getStrColumnUpdate(List<ColumnInfo> columnInfos) {
//		StringBuilder res = new StringBuilder();
//		for (ColumnInfo columnInfo : columnInfos) {
//			if (res.length() != 0) {
//				res.append(", ");
//			}
//			res.append(columnInfo.name + " = " + getCorrectValue(columnInfo));
//		}
//		return res.toString();
//	}
	
	
	/**
	 * Get str column in condition where
	 * @param condition
	 * @return String column condition (col1 = '5' AND col2 = '6')
	 */
//	public String getStrColUpdateCondition(List<ColumnInfo> condition) {
//		StringBuilder res = new StringBuilder();
//		for (ColumnInfo columnInfo : condition) {
//			if (res.length() != 0) {
//				res.append("AND ");
//			}
//			res.append(columnInfo.name + " = " + getCorrectValue(columnInfo));
//		}
//		return res.toString();
//	}
	
	/**
	 * Get correct value column
	 */
	private String getCorrectValue(String dataType, String value) {
		String type = dataType;
		String res = "";
		switch (type) {
		case "date":
			String val = value;
			if (value.indexOf("'") >= 0) {
				res = value;
			} else {
				if (val == null || val.isEmpty()) {
					// Get current date
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");  
					LocalDateTime now = LocalDateTime.now();  
					val = dtf.format(now);
				}
				res = "'" + val + "'";
			}
			break;
		case "varchar":
		case "nvarchar":
		case "char":
		case "nchar":
			// Has ''
			if (value.indexOf("'") >= 0) {
				res = value;
			} else {
				res = "'" + value + "'";
			}
			break;
		case "number":
		case "int":
		case "bigint":
			if (value == null || value.isEmpty()) {
				res = "0";
			} else {
				res = value;
			}
			break;
		default:
			System.out.println("Other?");
			break;
		}
		return res;
	}
}
