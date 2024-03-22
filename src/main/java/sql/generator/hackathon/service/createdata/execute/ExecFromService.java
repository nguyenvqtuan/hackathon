package sql.generator.hackathon.service.createdata.execute;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.springframework.stereotype.Service;

import sql.generator.hackathon.exception.NotFoundValueSQLException;
import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.Cond;
import sql.generator.hackathon.model.Condition;
import sql.generator.hackathon.model.ParseObject;
import sql.generator.hackathon.model.TableSQL;
import sql.generator.hackathon.model.createdata.InnerReturnObjectFrom;
import sql.generator.hackathon.model.createdata.InnerReturnObjectWhere;
import sql.generator.hackathon.model.createdata.NodeColumn;
import sql.generator.hackathon.model.createdata.ReturnObjectFrom;
import sql.generator.hackathon.model.createdata.ReturnObjectWhere;
import sql.generator.hackathon.model.createdata.constant.Constant;
import sql.generator.hackathon.service.ExecuteDBSQLServer;
import sql.generator.hackathon.service.createdata.CommonService;
import sql.generator.hackathon.service.createdata.ServiceCreateData;

@Service
public class ExecFromService {

	private Map<String, List<String>> keysFormat;
	private Map<String, String[]> infoCol;
	
	private ReturnObjectWhere returnObjWhere;
	
	private ReturnObjectFrom returnObjFrom;
	
	private CommonService commonService;
	
	private ExecuteDBSQLServer dbService;
	
	public ReturnObjectFrom processFrom(CommonService commonService, ExecuteDBSQLServer dbService, 
			ParseObject parseObject, ReturnObjectWhere returnObjWhere) throws SQLException {
		processInit(commonService, dbService, parseObject, returnObjWhere);
		Map<String, Set<String>> columnMapping = getMappingColumn();
		
		Map<String, List<Cond>> mappingAllColumn = calcLastMapping(getAllMappingColum(columnMapping));
		processCalcMappingKey(mappingAllColumn);
		
		return returnObjFrom;
	}
	
	private void processInit(CommonService commonService, ExecuteDBSQLServer dbService, 
			ParseObject parseObject, ReturnObjectWhere returnObjWhere) {
		initObject(commonService, dbService, returnObjWhere);
		List<TableSQL> tables = parseObject.getListTableSQL();
		Map<String, List<String>> mappingKey = parseObject.getMappingKey();
		tables.stream().forEach(x -> {
			String tableName = x.getTableName();
			String aliasName = x.getAlias();
			List<Condition> listCondition = x.getCondition();
			listCondition.stream()
			.filter(y -> y.getRight() != null && y.getRight().startsWith(Constant.STR_KEYS))
			.forEach(y -> {
				String[] arrTableColumn = commonService.getArrInColumns(y.getLeft());
				String tableAliasColumnName = commonService.getTableAliasColumnName(tableName + Constant.STR_DOT + aliasName + Constant.STR_DOT + arrTableColumn[2]);
				String right = y.getRight();
				if (!mappingKey.containsKey(right)) {
					throw new IllegalArgumentException("Mapping key not exists!");
				}
				processGetKeysFormat(right, tableAliasColumnName);
			});
		});
		
		processGetInfoColumn(parseObject.getListTableSQL());
	}
	
	private void initObject(CommonService commonService, ExecuteDBSQLServer dbService,
			ReturnObjectWhere returnObjWhere) {
		this.keysFormat = new HashMap<>();
		this.infoCol = new HashMap<>();
		this.returnObjWhere = returnObjWhere;
		this.returnObjFrom = new ReturnObjectFrom();
		this.returnObjFrom.setMappingTableAliasColumn(new HashMap<>());
		this.commonService = commonService;
		this.dbService = dbService;
	}
	
	/**
	 * Process get keys format
	 * @param right
	 * @param tableAliasColumnName
	 */
	private void processGetKeysFormat(String right, String tableAliasColumnName) {
		List<String> listKey;
		if (keysFormat.containsKey(right)) {
			listKey = keysFormat.get(right);
		} else {
			listKey = new ArrayList<>();
			keysFormat.put(right, listKey);
		}
		listKey.add(tableAliasColumnName);
	}
	
	/**
	 * Read Mapping in keys With each key add 2 mapping
	 * 
	 * @return Map<String, String> Key1 - Key2, Key2 - Key1
	 */
	private Map<String, Set<String>> getMappingColumn() {
		Map<String, Set<String>> m = new HashMap<>();
		keysFormat.entrySet().forEach(e -> {
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
		});
		return m;
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
//				if (saveCalcObj.containsKey(c[0])) {
//					saveCalcObj.get(c[0]).getListOperator().add(operators[0]);
//				} else {
//					CreateDataObj createObj = new CreateDataObj();
//					createObj.getListOperator().add(operators[0]);
//					saveCalcObj.put(c[0], createObj);
//				}
//				
//				if (saveCalcObj.containsKey(c[1])) {
//					saveCalcObj.get(c[1]).getListOperator().add(operators[1]);
//				} else {
//					CreateDataObj createObj = new CreateDataObj();
//					createObj.getListOperator().add(operators[1]);
//					saveCalcObj.put(c[1], createObj);
//				}
				
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
	private void processCalcMappingKey(Map<String, List<Cond>> columnMap) throws SQLException {
		// table.colName => all column mapping of current table.colName
		// Map<String, Set<Cond>> columnMap;
		
		Set<String> visitedMapping = new HashSet<>();
		// Map<String, List<Cond>> mapColumn = calcLastMapping();
		
		// table.aliasName.colName => condition valid
		// Map<String, List<Cond>> columnMap;
		for (Map.Entry<String, List<Cond>> e : columnMap.entrySet()) {
			String tableAliasColumnName = commonService.getTableAliasColumnName(e.getKey());
			
			String[] tableColumnName = commonService.getArrInColumns(tableAliasColumnName); 
			String tableName = tableColumnName[0];
			String colName = tableColumnName[2];
			
			// calculated this column!
			if (visitedMapping.contains(tableAliasColumnName)) {
				continue;
			}
			
			ColumnInfo t = commonService.getColumnInfo(tableName, colName);
			ColumnInfo colInfo = new ColumnInfo(t.getName(), "", t.getTypeName(), t.getTypeValue(),
						t.getIsNull(), t.getIsPrimarykey(), t.getIsForeignKey(), t.getUnique());

			String dataType = commonService.getCommonDataType(colInfo.getTypeName());
			int len = commonService.convertLength(colInfo.getTypeValue());
			
			InnerReturnObjectWhere innerReturnObjectWhere = returnObjWhere.getValueMappingTableAliasColumn().get(tableAliasColumnName);
			List<String> validOfCol = new ArrayList<>();
			boolean flgGenValue = true;
			if (innerReturnObjectWhere != null) {
				 if (innerReturnObjectWhere.getLastValue() != null && !innerReturnObjectWhere.getLastValue().isEmpty()) {
					 validOfCol = Arrays.asList(innerReturnObjectWhere.getLastValue());
					 flgGenValue = false;
				 } else {
					 if (innerReturnObjectWhere.getValidValueForColumn() != null && !innerReturnObjectWhere.getValidValueForColumn().isEmpty()) {
						// When not validValue for this column => free style this case.
						// Maybe data type, min-len => push default key for this.
						 validOfCol = innerReturnObjectWhere.getValidValueForColumn();
						 flgGenValue = false;
					}
				 }
			}
			
			if (flgGenValue) {
				validOfCol = processGenValue(tableName, colInfo, dataType, len);
			}
			// Save in there!
			// Use DFS confirm this case!
			Stack<NodeColumn> toExploder = new Stack<>();
			Map<NodeColumn, NodeColumn> parentMap = new HashMap<>();
			
			// Init 
			// index -> Cond in e.getValue()
			// 0 -> Cond in e.getValue()
			// 0 -> Cond = value => tableName.colName, operator => <=, >=, >, <, !=
			HashMap<Integer, Cond> loopSearch = new HashMap<>(e.getValue().size());
			int i = 0;
			for (Cond conD : e.getValue()) {
				loopSearch.put(i, conD);
				++i;
			}
			
			// Init
			i = validOfCol.size() - 1;
			for (; i >= 0; --i) {
				NodeColumn nodeCol = new NodeColumn(tableAliasColumnName, validOfCol.get(i), 0, loopSearch.get(0), null);
				toExploder.add(nodeCol);
			}
			
			NodeColumn nodeGoal = processCalcKeyMap(toExploder, parentMap, e.getValue(), validOfCol, 
					loopSearch, dataType);
			
			// Find valid value path
			// Next to insert to data table.
			// Flag true all Set<condtion> include this column! 
			// Will not execute again this link mapping.
			List<NodeColumn> pathValidValue = findPathValidForMapping(parentMap, nodeGoal);
			for (NodeColumn cur : pathValidValue) {
				// Mark color for column Info
				if (returnObjFrom.getMappingTableAliasColumn() != null) {
					if (returnObjFrom.getMappingTableAliasColumn().containsKey(cur.getTableAliasColumnName()) ) {
						returnObjFrom.getMappingTableAliasColumn().get(cur.getTableAliasColumnName()).setLastValue(cur.getVal());
						returnObjFrom.getMappingTableAliasColumn().get(cur.getTableAliasColumnName())
							.setMarkColor(Constant.KEY_MARK_COLOR + Constant.STR_UNDERLINE + ServiceCreateData.indexColor);
					} else {
						Map<String, InnerReturnObjectFrom> mappingTableAliasColumn = returnObjFrom.getMappingTableAliasColumn();
						InnerReturnObjectFrom innerReturnObjectFrom = new InnerReturnObjectFrom();
						innerReturnObjectFrom.setLastValue(cur.getVal());
						innerReturnObjectFrom.setMarkColor(Constant.KEY_MARK_COLOR + Constant.STR_UNDERLINE + ServiceCreateData.indexColor);
						mappingTableAliasColumn.put(cur.getTableAliasColumnName(), innerReturnObjectFrom);
					}
				} else {
					Map<String, InnerReturnObjectFrom> mappingTableAliasColumn = new HashMap<>();
					InnerReturnObjectFrom innerReturnObjectFrom = new InnerReturnObjectFrom();
					innerReturnObjectFrom.setLastValue(cur.getVal());
					innerReturnObjectFrom.setMarkColor(Constant.KEY_MARK_COLOR + Constant.STR_UNDERLINE + ServiceCreateData.indexColor);
					mappingTableAliasColumn.put(cur.getTableAliasColumnName(), innerReturnObjectFrom);
					returnObjFrom.setMappingTableAliasColumn(mappingTableAliasColumn);
				}
				visitedMapping.add(cur.getTableAliasColumnName());
			}
			ServiceCreateData.indexColor++;
		};
	}
	
	private Map<String, List<Cond>> calcLastMapping(Map<String, List<Cond>> columnMap) {
		Map<String, List<Cond>> res = new HashMap<>();
		Set<String> visited = new HashSet<>();
		for (Map.Entry<String, List<Cond>> x : columnMap.entrySet()) {
			String tableAliasColumnName = x.getKey();
			if (!visited.contains(tableAliasColumnName)) {
				List<Cond> mapping = x.getValue();
				InnerReturnObjectWhere InnerReturnObjectWhere = returnObjWhere.getValueMappingTableAliasColumn().get(tableAliasColumnName);
				if (InnerReturnObjectWhere == null) {
					continue;
				}
				List<String> valuesInWhereCondition = InnerReturnObjectWhere.getValidValueForColumn();
				if (valuesInWhereCondition != null) {
					visited.add(tableAliasColumnName);
					mapping.stream().forEach(y -> {
						visited.add(y.value);
						visited.add(y.rightValue);
					});
					res.put(tableAliasColumnName, x.getValue());
				}
			}
		}
		for (Map.Entry<String, List<Cond>> x : columnMap.entrySet()) {
			String tableAliasColumnName = x.getKey();
			if (!visited.contains(tableAliasColumnName)) {
				res.put(tableAliasColumnName, x.getValue());
			}
		}
		return res;
	}
	
	/**
	 * Process gen value for mapping key
	 * @param tableName
	 * @param columnInfo
	 * @param dataType
	 * @param length
	 * @return
	 * @throws SQLException 
	 */
	private List<String> processGenValue(String tableName, ColumnInfo columnInfo,
			String dataType, int length) throws SQLException {
		List<String> res = new ArrayList<>();
		if (columnInfo.isKey()) {
			res.addAll(dbService.genListUniqueVal(tableName, columnInfo, "", ""));
		} else {
			res.addAll(commonService.processGenValue(dataType, length, "", ""));
		}
		return res;
	}
	
	/**
	 * Process calculator for key mapping
	 * @throws SQLException 
	 */
	private NodeColumn processCalcKeyMap(Stack<NodeColumn> toExploder, 
			Map<NodeColumn, NodeColumn> parentMap, List<Cond> colMapping, 
			List<String> validOfCol, Map<Integer, Cond> loopSearch, 
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
				if (tmp.getTableAliasColumnName().equals(nextCond.rightValue) || 
						tmp.getTableAliasColumnName().equals(nextCond.value)) {
					val = tmp.val;
				}
				tmp = parentMap.get(tmp);
			}
			
			// Remove value generator
			// Just calculator first meet index
			// Valid value will increase
			if (!checkMeet[index]) {
				// When has condition will remove current				
				if (returnObjWhere.getValueMappingTableAliasColumn() != null && 
						returnObjWhere.getValueMappingTableAliasColumn().containsKey(nextCond.value)) {
					List<String> listValidValue = returnObjWhere.getValueMappingTableAliasColumn().get(nextCond.value).getValidValueForColumn();
					if (listValidValue != null && !listValidValue.isEmpty()) {
						validOfCol = listValidValue;
					}

					String valueInEquals = returnObjWhere.getValueMappingTableAliasColumn().get(nextCond.value).getLastValue();
					if (valueInEquals != null && !valueInEquals.isEmpty()) {
						validOfCol = Arrays.asList(valueInEquals);
					}
				}
				
				// Not found valid for mapping column
				if (validOfCol.isEmpty()) {
					throw new NotFoundValueSQLException("Not found valid value for this SQL!");
				}
			}
			
			checkMeet[index] = true;
			
			for (int i = validOfCol.size() - 1; i >= 0; --i) {
				
				String[] innerTableColName = commonService.getArrInColumns(nextCond.value);
				ColumnInfo t2 = commonService.getColumnInfo(innerTableColName[0], innerTableColName[2]);
				ColumnInfo colInnerInfo = new ColumnInfo(t2.getName(), "", 
						t2.getTypeName(), t2.getTypeValue(), t2.getIsNull(), 
						t2.getIsPrimarykey(), t2.getIsForeignKey(), t2.getUnique());
				
				boolean flgAdd = isKeyMapping(nextCond, val, validOfCol.get(i), dataType);

				Map<String, String> valCompositeKey = null; 
				if (flgAdd) {
					// Check value unique
					if ((colInnerInfo.isKey() || colInnerInfo.getUnique()) && 
							!dbService.isUniqueValue(innerTableColName[0], colInnerInfo, validOfCol.get(i))) {
						flgAdd = false;
					} else {
						flgAdd = true;
					}
					boolean innerIsCompositeKey = commonService.isCompositeKey(innerTableColName[0]);
					
					// Execute for composite key
					if (flgAdd && innerIsCompositeKey) {
						valCompositeKey = dbService
								.genUniqueCol(commonService.objCommon.getObjectGenate().getInfoDatabase().getSchema(), 
								innerTableColName[0], colInnerInfo, validOfCol.get(i));
						if (valCompositeKey.size() != 0) {
							flgAdd = true;
						} else {
							flgAdd = false;
						}
					}
				}
				
				if (flgAdd) {
					// Table columnName, value, index
					NodeColumn innerNode = new NodeColumn(nextCond.value.equals(curNode.getTableAliasColumnName()) ? nextCond.rightValue : nextCond.value, 
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
		case Constant.EXPRESSION_EQUALS:
			if (currentVal.equals(checkVal)) {
				flgAdd = true;
			}
			break;
		case Constant.EXPRESSION_LESS_EQUALS:
			if (dataType.equals(Constant.STR_TYPE_DATE)) {
				Date tmp1 = commonService.convertStringToDate(currentVal);
				Date tmp2 = commonService.convertStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) <= 0) {
					flgAdd = true;
				}
			} else if (dataType.equals(Constant.STR_TYPE_NUMBER)) {
				Integer int1 = commonService.convertStringToInt(currentVal);
				Integer int2 = commonService.convertStringToInt(checkVal);
				if (int2 <= int1) {
					flgAdd = true;
				}
			}
			// number
			break;
		case Constant.EXPRESSION_GREATER_EQUALS:
			if (dataType.equals(Constant.STR_TYPE_DATE)) {
				// date
				Date tmp1 = commonService.convertStringToDate(currentVal);
				Date tmp2 = commonService.convertStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) >= 0) {
					flgAdd = true;
				}
			} else if (dataType.equals(Constant.STR_TYPE_NUMBER)) {
				Integer int1 = commonService.convertStringToInt(currentVal);
				Integer int2 = commonService.convertStringToInt(checkVal);
				if (int2 >= int1) {
					flgAdd = true;
				}
			}
			break;
		case Constant.EXPRESSION_LESS:
			if (dataType.equals(Constant.STR_TYPE_DATE)) {
				// date
				Date tmp1 = commonService.convertStringToDate(currentVal);
				Date tmp2 = commonService.convertStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) < 0) {
					flgAdd = true;
				}
			} else if (dataType.equals(Constant.STR_TYPE_NUMBER)) {
				Integer int1 = commonService.convertStringToInt(currentVal);
				Integer int2 = commonService.convertStringToInt(checkVal);
				if (int2 < int1) {
					flgAdd = true;
				}
			}
			break;
		case Constant.EXPRESSION_GREATER:
			if (dataType.equals(Constant.STR_TYPE_DATE)) {
				// date
				Date tmp1 = commonService.convertStringToDate(currentVal);
				Date tmp2 = commonService.convertStringToDate(checkVal);
				if (tmp2.compareTo(tmp1) > 0) {
					flgAdd = true;
				}
			} else if (dataType.equals(Constant.STR_TYPE_NUMBER)) {
				Integer int1 = commonService.convertStringToInt(currentVal);
				Integer int2 = commonService.convertStringToInt(checkVal);
				if (int2 > int1) {
					flgAdd = true;
				}
			}
			break;
		case Constant.EXPRESSION_DIFF_1:
		case Constant.EXPRESSION_DIFF_2:
			if (!currentVal.equals(checkVal)) {
				flgAdd = true;
			}
			break;
		default:
			throw new IllegalArgumentException("Not have other condition!");
		}
		return flgAdd;
	}
	
	/**
	 * Find path valid in parentMap from node goal.
	 * @param parentMap
	 * @param nodeGoal
	 * @return
	 */
	private List<NodeColumn> findPathValidForMapping(Map<NodeColumn, NodeColumn> parentMap, 
			NodeColumn nodeGoal) {
		List<NodeColumn> res = new ArrayList<>();
		NodeColumn curNode = nodeGoal;
		while (curNode != null) {
			res.add(curNode);
			curNode = parentMap.get(curNode);
		}
		return res;
	}
	
	private void processGetInfoColumn(List<TableSQL> tables) {
		for (TableSQL tableSQL : tables) {
			for (Condition cd : tableSQL.getCondition()) {
				if (cd.getRight() == null || !cd.getRight().startsWith(Constant.STR_KEYS)) {
					continue;
				}
				String tableAliasName = commonService.getTableAliasName(tableSQL.getTableName() + Constant.STR_DOT + tableSQL.getAlias());
				String[] arrTableAliasColumnName = commonService.getArrInColumns(cd.getLeft());
				String tableAliasColumnName = tableAliasName + Constant.STR_DOT + arrTableAliasColumnName[2];
				String tableAliasColumnName2 = "";
				for (String t : keysFormat.get(cd.getRight())) {
					if (!t.equals(tableAliasColumnName)) 
//						tableAliasColumnName2 = commonService.getTableAndColName(t)[0] + "." + commonService.getTableAndColName(t)[1];
						tableAliasColumnName2 = t;
				}
				processCalInfoCol(tableAliasColumnName, tableAliasColumnName2, cd.getExpression());
			}
		}
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
}
