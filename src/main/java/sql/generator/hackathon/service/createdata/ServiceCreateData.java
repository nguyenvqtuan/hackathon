package sql.generator.hackathon.service.createdata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.Condition;
import sql.generator.hackathon.model.CreateObject;
import sql.generator.hackathon.model.InforTableReferFK;
import sql.generator.hackathon.model.ObjectGenate;
import sql.generator.hackathon.model.ParseObject;
import sql.generator.hackathon.model.TableSQL;
import sql.generator.hackathon.model.createdata.InnerReturnObjectFrom;
import sql.generator.hackathon.model.createdata.InnerReturnObjectWhere;
import sql.generator.hackathon.model.createdata.ReturnObjectFrom;
import sql.generator.hackathon.model.createdata.ReturnObjectWhere;
import sql.generator.hackathon.model.createdata.constant.Constant;
import sql.generator.hackathon.service.ExecuteDBSQLServer;
import sql.generator.hackathon.service.ServerFaker;
import sql.generator.hackathon.service.createdata.execute.ExecClientService;
import sql.generator.hackathon.service.createdata.execute.ExecFromService;
import sql.generator.hackathon.service.createdata.execute.ExecInAndNotInService;
import sql.generator.hackathon.service.createdata.execute.ExecWhereService;

@Service
public class ServiceCreateData {

	@Autowired
	private ExecWhereService execWhereService;
	
	@Autowired
	private ExecFromService execFromService;
	
	@Autowired
	private ExecClientService execClientService;
	
	@Autowired
	private ExecInAndNotInService execInAndNotInService;
	
	@Autowired
	private ServerFaker fakerService;
	
	@Autowired
	public ExecuteDBSQLServer dbService;
	
	@Autowired
	public CommonService commonService;
	
	private Map<String, InforTableReferFK> foreignKeyNotExistsInmainTable;
	
	private static String locale;
	
	public static int indexColor;
	
	/**
	 * Call first
	 * @param objectGenate
	 * @param parseObject
	 * @param dataPicker
	 * @param rowCreate
	 * @param flagInsert
	 * @throws SQLException
	 */
	public CreateObject process(ExecuteDBSQLServer executeDBServer, ObjectGenate objectGenate, ParseObject parseObject, 
			Map<String, List<List<ColumnInfo>>> dataPicker, boolean flgInsert, String locale) throws SQLException {
		CreateObject response = new CreateObject();
		try {
			this.locale = locale;
			int rowCreate = objectGenate.getRow();
			init(executeDBServer, objectGenate, parseObject);
			
			
			ReturnObjectWhere objWhere = execWhereService.processWhere(commonService, parseObject);
			ReturnObjectFrom objFrom = execFromService.processFrom(commonService, executeDBServer, 
					parseObject, objWhere);
			
			Map<String, List<ColumnInfo>> lastValue = processCalcLastValue(objFrom, objWhere);
			
			response = processMultipleRow(lastValue, dataPicker, rowCreate, flgInsert);
		} catch (Exception e) {
 			e.printStackTrace();
		} finally {
			// Close connection
			if (dbService != null) {
				dbService.disconnectDB();
			}
		}
		return response;
	}
	
	private void init(ExecuteDBSQLServer executeDBServer, ObjectGenate objectGenate, 
			ParseObject parseObject) throws Exception {
		indexColor = 1;
		
		foreignKeyNotExistsInmainTable = new HashMap<>();

		commonService.init(executeDBServer, objectGenate, parseObject);
		execClientService.init(commonService, parseObject);
		processFormatTableSQL(parseObject.getListTableSQL());
	}
	
	private void processFormatTableSQL(List<TableSQL> tables) {
		for (TableSQL table : tables) {
			List<Condition> conditions = table.getCondition();
			for (Condition cond : conditions) {
				if (cond.getRight() != null && !cond.getRight().startsWith(Constant.STR_KEYS)) {
					cond.setRight(commonService.removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, cond.getRight()));
				}
				
				if (cond.getListRight() != null) {
					List<String> reListRight = new ArrayList<>();
					for (String val : cond.getListRight()) {
						reListRight.add(commonService.removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, val));
					}
					cond.setListRight(reListRight);
				}
			}
		}
	}
	
	private Map<String, List<ColumnInfo>> processCalcLastValue(ReturnObjectFrom objFrom, ReturnObjectWhere objWhere) {
		Map<String, List<ColumnInfo>> res = new HashMap<>();
		Map<String, List<ColumnInfo>> tableInfo = commonService.objCommon.getTableInfo();
		for (Map.Entry<String, List<ColumnInfo>> x : tableInfo.entrySet()) {
			String tableNameInfo = x.getKey();
			List<ColumnInfo> listColumnInfo = x.getValue();
			for (ColumnInfo y : listColumnInfo) {
				String aliasTableInfo = y.tableAlias;
				String columnNameInfo = y.name;
				String tableAliasColumnName = commonService.getTableAliasColumnName(tableNameInfo + Constant.STR_DOT + aliasTableInfo + Constant.STR_DOT + columnNameInfo);
				
				ColumnInfo resColumnInfo = new ColumnInfo();
				resColumnInfo.setName(columnNameInfo);
				String lastValue = "";
				String markColor = "";
				InnerReturnObjectFrom innerReturnObjFrom = objFrom.getMappingTableAliasColumn().get(tableAliasColumnName);
				if (innerReturnObjFrom != null) {
					if (innerReturnObjFrom.getLastValue() != null && !innerReturnObjFrom.getLastValue().isEmpty()) {
						lastValue = innerReturnObjFrom.getLastValue();
						markColor = innerReturnObjFrom.getMarkColor();
					} else if (innerReturnObjFrom.getListValidValue() != null && !innerReturnObjFrom.getListValidValue().isEmpty()){
						lastValue = innerReturnObjFrom.getListValidValue().get(0);
						markColor = innerReturnObjFrom.getMarkColor();
					} else {
						throw new IllegalArgumentException("Not contains values for condition!");
					}
				} else {
					InnerReturnObjectWhere innerReturnObjWhere = objWhere.getValueMappingTableAliasColumn().get(tableAliasColumnName);
					if (innerReturnObjWhere != null) {
						if (innerReturnObjWhere.getLastValue() != null && !innerReturnObjWhere.getLastValue().isEmpty()) {
							lastValue = innerReturnObjWhere.getLastValue();
							markColor = innerReturnObjWhere.getMarkColor();
						} else if (innerReturnObjWhere.getValidValueForColumn() != null && !innerReturnObjWhere.getValidValueForColumn().isEmpty()){
							lastValue = innerReturnObjWhere.getValidValueForColumn().get(0);
							markColor = innerReturnObjWhere.getMarkColor();
						} else {
							ColumnInfo colInfo = commonService.getColumnInfo(tableNameInfo, columnNameInfo);
							String typeName = colInfo.getTypeName();
							int typeValue = commonService.convertLength(colInfo.getTypeValue());
							List<String> inValidValues = innerReturnObjWhere.getInValidValueForColumn();
							lastValue = execInAndNotInService.processNotIn(commonService.processGenValue(typeName, typeValue, "", ""), 
									inValidValues).get(0);
							markColor = Constant.KEY_MARK_COLOR + Constant.STR_UNDERLINE + Constant.DEFAULT_NUM_MARK_COLOR;
						}
					}
				}
				resColumnInfo.setVal(lastValue);
				resColumnInfo.setColor(markColor);
				resColumnInfo.setTableAlias(aliasTableInfo);
				List<ColumnInfo> resListColumnInfo;
				if (res.containsKey(tableNameInfo)) {
					resListColumnInfo = res.get(tableNameInfo);
				} else {
					resListColumnInfo = new ArrayList<>();
					res.put(tableNameInfo, resListColumnInfo);
				}
				resListColumnInfo.add(resColumnInfo);
			}
		}
		return res;
	}
	
	private CreateObject processMultipleRow(Map<String, List<ColumnInfo>> lastValue,
			Map<String, List<List<ColumnInfo>>> dataPicker, int row, boolean flgInsert) throws Exception {
		Map<String, List<List<ColumnInfo>>> responseListData = new HashMap<>();
		List<String> listMarkColor = new ArrayList<>();
		for (int idxRow = 1; idxRow <= row; ++idxRow) {
			Map<String, List<ColumnInfo>> dataOneRow = processOneRow(lastValue, dataPicker, idxRow, listMarkColor);
			dataOneRow.entrySet().forEach(m -> {
				String tableName = m.getKey();
				List<List<ColumnInfo>> t;
				if (responseListData.containsKey(tableName)) {
					t = responseListData.get(tableName);
				} else {
					t = new ArrayList<>();
					responseListData.put(tableName, t);
				}
				t.add(m.getValue());
			});
		}
		processWithForeignKey(responseListData);
		CreateObject createObj = new CreateObject();
		createObj.setListData(responseListData);
		createObj.setListMarkColor(listMarkColor);
		return createObj;
	}
	
	private Map<String, List<ColumnInfo>> processOneRow(Map<String, List<ColumnInfo>> lastValueTable,
			Map<String, List<List<ColumnInfo>>> dataPicker,
			int idxRow, List<String> listMarkColor) throws Exception {
		Map<String, List<ColumnInfo>> res = new HashMap<>();
		Map<String, List<ColumnInfo>> tableInfo = commonService.objCommon.getTableInfo();
		for (Map.Entry<String, List<ColumnInfo>> e : tableInfo.entrySet()) {
			String tableName = e.getKey();
			List<ColumnInfo> l = new ArrayList<>();
			Set<String> hasColumn = new HashSet<>();
			List<String> listAliasTable = new ArrayList<>();
			// Init
			for (ColumnInfo colInfo : e.getValue()) {
				if (!hasColumn.contains(colInfo.getName())) {
					ColumnInfo innerColumnInfo = new ColumnInfo(colInfo.getName(), "", colInfo.getTypeName(),
							colInfo.getTypeValue(), colInfo.getIsNull(), colInfo.getIsPrimarykey(),
							colInfo.getIsForeignKey(), colInfo.getUnique());
					innerColumnInfo.setTableAlias(colInfo.getTableAlias());
					listAliasTable.add(colInfo.getTableAlias());
					l.add(innerColumnInfo);
					hasColumn.add(colInfo.getName());
				}
			}
			
			List<ColumnInfo> data = lastValueTable.get(tableName);
			
			// confirm KEY no value
			ColumnInfo colNoVal = null;
						
			if (data != null && !data.isEmpty()) {
				for (ColumnInfo colInfo : l) {
					for (ColumnInfo d : data) {
						if (colInfo.getName().equals(d.getName()) && colInfo.getTableAlias().equals(d.getTableAlias())) {
							colInfo.setVal(d.getVal());
							colInfo.setColor(d.getColor());
						}
					}
					
					if (colInfo.isKey() && (colInfo.getVal() == null || colInfo.getVal().isEmpty())) {
						colNoVal = colInfo;
					}
				}
			} 
			
			if (colNoVal != null) {
				Map<String, ColumnInfo> mapVal = genValueForKey(tableName, colNoVal);
				for (ColumnInfo colInfo : l) {
					if (colInfo.isKey() && (colInfo.getVal() == null || colInfo.getVal().isEmpty())) {
						colInfo.setVal(commonService.removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, mapVal.get(tableName + Constant.STR_DOT + colInfo.getName()).getVal()));
					}
				}
			}
			
			// TODO
			// Prepare for next version
			// Update meaningful value
			List<String> listCurrentUnique = new ArrayList<>();
			
			// Get unique val
			for (ColumnInfo colInfo : l) {
				if (colInfo.getUnique() && (colInfo.getVal() == null || colInfo.getVal().isEmpty())) {
					listCurrentUnique = dbService.getListUniqueVal(tableName, colInfo);
					colInfo.setVal(commonService.removeSpecifyCharacterFirstLastStr(Constant.CHAR_APOSTROPHE, dbService.genListUniqueVal(tableName, colInfo, "", "").get(0)));
				}
			}

			List<String> listAliasTable2 = commonService.objCommon.getListTableAlias().get(tableName);
			if (listAliasTable2 != null) {
				listAliasTable = commonService.processMergeList(listAliasTable, listAliasTable2);
			}
			
			listAliasTable.stream().forEach(x -> execClientService.addColumnGetFromSelect(l, x));
			execClientService.setClientData(tableName, idxRow, l, dataPicker);
			
			// Add default value
			for (ColumnInfo colInfo : l) {
				if (colInfo.getVal() == null || colInfo.getVal().isEmpty()) {
					String dataType = colInfo.getTypeName().equals(Constant.STR_TYPE_DATE) ? Constant.STR_TYPE_DATE : ""; 
					colInfo.setVal(fakerService.getDataByColumn(colInfo.getName(), dataType, this.locale));	
				}
			}
			
			// Check foreign key has exists
			for (ColumnInfo colInfo : l) {
				if (colInfo.getIsForeignKey()) {
					InforTableReferFK foreingKeyInfo = dbService.checkInforFK(
							commonService.objCommon.getObjectGenate().getInfoDatabase().getSchema(), 
							commonService.removeSpecifyCharacter("'", tableName), colInfo);
					if (!foreingKeyInfo.isHasExist()) {
						foreignKeyNotExistsInmainTable.put(foreingKeyInfo.getTableReferFKName(), foreingKeyInfo);
					}
				}
			}
			
			// Set list markColor
			l.stream().filter(y -> !listMarkColor.contains(y.getColor())).forEach(y -> {
				listMarkColor.add(y.getColor());
			});
			
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
	private Map<String, ColumnInfo> genValueForKey(String tableName, ColumnInfo colInfo) throws SQLException {
		Map<String, ColumnInfo> res = new HashMap<>();
		
		List<String> listVal = dbService.genListUniqueVal(tableName, colInfo, "", "");
		
		// Gen value for key with no condition
		if (!commonService.isCompositeKey(tableName)) {
			res.put(tableName + "." + colInfo.name, new ColumnInfo(colInfo.name, listVal.get(0)));
		} else {
			for (String val : listVal) {
				Map<String, String> m = dbService.genUniqueCol(commonService.objCommon.getObjectGenate().getInfoDatabase().getSchema(), tableName, colInfo, val);
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
