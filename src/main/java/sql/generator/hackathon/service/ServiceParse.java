package sql.generator.hackathon.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.Condition;
import sql.generator.hackathon.model.InfoDisplayScreen;
import sql.generator.hackathon.model.ParseObject;
import sql.generator.hackathon.model.TableSQL;

@Service
public class ServiceParse {
	private Map<String, TableSQL> tables;
	private Map<String, List<String>> parentAlias;
	private Map<String, List<String>> mappingKey;
	private static Map<String, String> reverseExpression = new HashMap<>();
	private List<Condition> listCondition;
	private Map<String, Set<ColumnInfo>> listColumnInfo;

	public static final String NOT_IN = "NOT IN";
	public static final String IN = "IN";
	public static final String GREATHER_OR_EQUAL = ">=";
	public static final String LESS_OR_EQUAL = "<=";
	public static final String GREATHER = ">";
	public static final String LESS = "<";
	public static final String EQUAL = "=";
	public static final String EQUAL_NOT = "!=";
	public static final String DOT = ".";
	public static final String LIKE = "LIKE";
	private int state = 1;

	private Map<String, Set<String>> listColumnAlias;

	static {
		reverseExpression.put("=", "!=");
		reverseExpression.put("!=", "=");
		reverseExpression.put("<", ">");
		reverseExpression.put(">", "<");
		reverseExpression.put(">=", "<");
		reverseExpression.put("<=", ">");
	}

	public Map<String, TableSQL> getColumnInfo(String query) throws JSQLParserException {
		String result[] = query.split(";");
		tables = new HashMap<>();
		for (String querySingle : result) {
			Statement statement = CCJSqlParserUtil.parse(querySingle);
			if (statement instanceof CreateTable) {
				CreateTable createTable = (CreateTable) statement;
				TableSQL tableSQL = TableSQL.builder().tableName(createTable.getTable().getName()).build();
				tableSQL.columns = createTable.getColumnDefinitions().stream().map(item -> item.getColumnName())
						.collect(Collectors.toSet());
				tableSQL.condition = new ArrayList<>();
				tables.put(tableSQL.getTableName(), tableSQL);
			}
			if (statement instanceof Insert) {
				Insert insert = (Insert) statement;
				TableSQL tableSQL = TableSQL.builder().tableName(insert.getTable().getName()).build();
				tableSQL.columns = insert.getColumns().stream().map(item -> item.getName(false))
						.collect(Collectors.toSet());
				tableSQL.condition = new ArrayList<>();
				tables.put(tableSQL.getTableName(), tableSQL);
			}
			if (statement instanceof Select) {
				Select select = (Select) statement;
				listColumnInfo = new HashMap<>();
				parentAlias = new HashMap<>();
				listColumnAlias = new HashMap<>();
				SelectBody selectBody = select.getSelectBody();
				processSelectBody(selectBody, null);
				processColumnWithAlias();
				return tables;
			}
		}

		return tables;
	}

	public List<InfoDisplayScreen> getColumnInfoView(String query) throws JSQLParserException {
		String result[] = query.split(";");
		List<InfoDisplayScreen> listInfoDisplayScreen = new ArrayList<>();
		for (String querySingle : result) {
			Statement statement = CCJSqlParserUtil.parse(querySingle);
			if (statement instanceof Insert) {
				Insert insert = (Insert) statement;
				InfoDisplayScreen infoDisplayScreen = new InfoDisplayScreen();
				infoDisplayScreen.tableName = insert.getTable().getName();
				infoDisplayScreen.listColumnName = insert.getColumns().stream().map(item -> item.getName(false))
						.collect(Collectors.toList());
				listInfoDisplayScreen.add(infoDisplayScreen);
			}
			if (statement instanceof CreateTable) {
				CreateTable createTable = (CreateTable) statement;
				InfoDisplayScreen infoDisplayScreen = new InfoDisplayScreen();
				infoDisplayScreen.tableName = createTable.getTable().getName();
				infoDisplayScreen.listColumnName = createTable.getColumnDefinitions().stream()
						.map(item -> item.getColumnName()).collect(Collectors.toList());
				listInfoDisplayScreen.add(infoDisplayScreen);
			}
			if (statement instanceof Select) {
				Select select = (Select) statement;
				listColumnInfo = new HashMap<>();
				tables = new HashMap<>();
				parentAlias = new HashMap<>();
				listColumnAlias = new HashMap<>();
				SelectBody selectBody = select.getSelectBody();
				processSelectBody(selectBody, null);
				processColumnWithAlias();
				List<InfoDisplayScreen> listInfo = new ArrayList<>();
				tables.entrySet().forEach(x -> {
					InfoDisplayScreen infoDisplayScreen = new InfoDisplayScreen();
					infoDisplayScreen.tableName = tables.get(x.getKey()).getTableName();
					if (x.getValue().getColumns() != null) {
						infoDisplayScreen.listColumnName = x.getValue().getColumns().stream()
								.collect(Collectors.toList());
					}
					listInfo.add(infoDisplayScreen);
				});
				return listInfo;
			}
		}
		return listInfoDisplayScreen;
	}

	private void processColumnWithAlias() {
		listColumnAlias.entrySet().forEach(x -> {
			TableSQL tbl = tables.get(x.getKey());
			Set<String> listTMP = new HashSet<>();
			if (tbl != null) {
				if (tbl.columns != null) {
					listTMP = tbl.columns;
				}
				listTMP.addAll(x.getValue());
				tbl.setColumns(listTMP);
				tables.put(x.getKey(), tbl);
			} else {
				List<String> alias = parentAlias.get(x.getKey());
				if (alias != null && !alias.isEmpty()) {
					for (String s : alias) {
						tbl = tables.get(s);
						if (tbl != null) {
							if (tbl.columns != null) {
								listTMP = tbl.columns;
							}

							listTMP.addAll(x.getValue());
							tbl.setColumns(listTMP);
							tables.put(s, tbl);
						}

					}
				}
			}
		});
	}

	private void processSelectBody(SelectBody selectBody, Alias alias) throws JSQLParserException {
		if (selectBody instanceof PlainSelect) {
			processSingle((PlainSelect) selectBody, alias);
		} else if (selectBody instanceof WithItem) {
		} else {
			SetOperationList operationList = (SetOperationList) selectBody;
			if (operationList.getSelects() != null && !operationList.getSelects().isEmpty()) {
				List<SelectBody> plainSelects = operationList.getSelects();
				for (SelectBody plainSelect : plainSelects) {
					processSelectBody(plainSelect, alias);
				}
			}
		}
	}

	private void processSelectItem(List<SelectItem> selectItems, String alias) {
		if (selectItems != null) {
			selectItems.forEach(item -> {
				if (item instanceof AllColumns) {
					System.out.println("ALL COLUMN" + item);
				}

				if (item instanceof SelectExpressionItem) {
					SelectExpressionItem selectExpressionItem = (SelectExpressionItem) item;
					if (selectExpressionItem.getExpression() instanceof Column) {
						Column column = (Column) selectExpressionItem.getExpression();

						String aliasOrTableName = Optional.ofNullable(column.getTable()).orElse(new Table(alias))
								.getName();

						Set<String> listColumn = listColumnAlias.getOrDefault(aliasOrTableName, new HashSet<>());

						listColumn.add(column.getColumnName());

						listColumnAlias.put(aliasOrTableName, listColumn);
					}

					if (selectExpressionItem.getExpression() instanceof SubSelect) {
						SelectBody selectBody = ((SubSelect) selectExpressionItem.getExpression()).getSelectBody();
						try {
							processSelectBody(selectBody, null);
						} catch (JSQLParserException e) {
							e.printStackTrace();
						}
					}
				}

			});
		}
	}

	private void processSingle(PlainSelect plainSelect, Alias alias) throws JSQLParserException {
		String tableNameORAlias;
		if (plainSelect.getFromItem() instanceof Table) {
			Table table = (Table) plainSelect.getFromItem();
			tableNameORAlias = table.getAlias() != null ? table.getAlias().getName() : table.getName();
			processSelectItem(plainSelect.getSelectItems(), tableNameORAlias);
		} else {
			if (plainSelect.getFromItem().getAlias() == null) {
				throw new JSQLParserException();
			}
			tableNameORAlias = plainSelect.getFromItem().getAlias().getName();
		}
		processFrom(plainSelect.getFromItem(), alias);
		List<Join> joins = plainSelect.getJoins();
		if (joins != null && !joins.isEmpty()) {
			joins.forEach(join -> {
				try {
					processJoinColumn(join, plainSelect.getFromItem(), null);
				} catch (JSQLParserException e) {
					e.printStackTrace();
				}
			});
		}
		if (plainSelect.getWhere() != null) {
			processExpression(plainSelect.getWhere(), plainSelect.getFromItem());
		}

	}

	private void processUsingJoin(Column column, Join join, FromItem fromItem) {
		String aliasLeft = fromItem.getAlias() != null ? fromItem.getAlias().getName() : fromItem.toString();
		String aliasRight = join.getRightItem().getAlias() != null ? join.getRightItem().getAlias().getName()
				: join.getRightItem().toString();
		Set<String> listLeft = listColumnAlias.get(aliasLeft);
		if (listLeft == null) {
			listLeft = new HashSet<>();
		}
		listLeft.add(column.getColumnName());
		listColumnAlias.put(aliasLeft, listLeft);
		Set<String> listRight = listColumnAlias.get(aliasRight);
		if (listRight == null) {
			listRight = new HashSet<>();
		}
		listRight.add(column.getColumnName());
		listColumnAlias.put(aliasRight, listRight);
	}

	private void processJoinColumn(Join join, FromItem fromItem, List<SelectItem> selectItems)
			throws JSQLParserException {
		if (join.getRightItem() instanceof SubSelect) {
			SubSelect subSelect = (SubSelect) join.getRightItem();
			processExpression(join.getOnExpression(), fromItem);
			processSelectBody(subSelect.getSelectBody(), null);

		} else {
			processExpression(join.getOnExpression(), fromItem);
			processTable(join.getRightItem());
			if (join.getUsingColumns() != null && !join.getUsingColumns().isEmpty()) {
				processUsingJoin(join.getUsingColumns().get(0), join, fromItem);
			}

			String tableNameORAlias = join.getRightItem().getAlias() != null ? join.getRightItem().getAlias().getName()
					: join.getRightItem().toString();
			if (selectItems != null && !selectItems.isEmpty()) {
				listColumnAlias.put(tableNameORAlias.replace(" ", ""),
						selectItems.stream().map(x -> x.toString()).collect(Collectors.toSet()));
			}
		}
	}

	private void processExpression(Expression expression, FromItem fromItem) throws JSQLParserException {
		if (expression != null) {
			if (expression instanceof NotExpression) {
				NotExpression not = (NotExpression) expression;
				processExpression(not.getExpression(), null);
			}
			if (expression instanceof ExistsExpression) {
				ExistsExpression ex = (ExistsExpression) expression;
				SubSelect sub = (SubSelect) ex.getRightExpression();
				processSelectBody(sub.getSelectBody(), null);
			}
			expression.accept(new ExpressionVisitorAdapter() {
				@Override
				protected void visitBinaryExpression(BinaryExpression expr) {

					if (expr.getLeftExpression() instanceof Column) {

						Column column = (Column) expr.getLeftExpression();
						String aliasOrTable = column.getTable() == null
								? fromItem.getAlias() != null ? fromItem.getAlias().getName() : fromItem.toString()
								: column.getTable().getAlias() != null ? column.getTable().getAlias().getName()
										: column.getTable().getName();
						Set<String> listTMP = listColumnAlias.get(aliasOrTable);
						if (listTMP == null) {
							listTMP = new HashSet<>();
						}
						listTMP.add(column.getColumnName());
						listColumnAlias.put(aliasOrTable.replace(" ", ""), listTMP);
					}
					if (expr.getRightExpression() instanceof Column) {
						Column column = (Column) expr.getRightExpression();
						String aliasOrTable = column.getTable() == null
								? fromItem.getAlias() != null ? fromItem.getAlias().getName() : fromItem.toString()
								: column.getTable().getAlias() != null ? column.getTable().getAlias().getName()
										: column.getTable().getName();
						Set<String> listTMP = listColumnAlias.get(aliasOrTable);
						if (listTMP == null) {
							listTMP = new HashSet<>();
						}
						listTMP.add(column.getColumnName());
						listColumnAlias.put(aliasOrTable.replace(" ", ""), listTMP);
					}
					super.visitBinaryExpression(expr);
				}
			});
		}

	}

	private void processFrom(FromItem fromItem, Alias alias) throws JSQLParserException {
		if (fromItem instanceof SubSelect) {
			SubSelect subSelect = (SubSelect) fromItem;
			processSelectBody(subSelect.getSelectBody(), alias != null ? alias : fromItem.getAlias());
		} else {
			processTable(fromItem);
			if (alias != null) {
				processAlias(alias, fromItem);
			}
		}
	}

	private void processTableColumn(FromItem fromItem) {
		Table table = (Table) fromItem;
		String alias = table.getAlias() != null ? table.getAlias().getName() : table.getName();
		Set<ColumnInfo> columnInfo = listColumnInfo.get(alias);
		if (columnInfo == null) {
			columnInfo = new HashSet<>();
		}
		listColumnInfo.put(alias, columnInfo);
	}

	public List<String> dataToSqlInsert(Map<String, List<List<ColumnInfo>>> listData) {

		List<String> listSQL = new ArrayList<>();
		for (java.util.Map.Entry<String, List<List<ColumnInfo>>> s : listData.entrySet()) {
			s.getValue().forEach(x -> {
				Insert insert = new Insert();
				Table table = new Table(s.getKey().toString());
				List<Column> columnList = new ArrayList<>();
				ExpressionList values = new ExpressionList();
				for (ColumnInfo columnInfo : x) {
					columnList.add(new Column(columnInfo.name));
					values.addExpressions(new StringValue(columnInfo.val));
				}
				insert.setTable(table);
				insert.setItemsList(values);
				insert.setColumns(columnList);
				listSQL.add(insert.toString());
			});

		}

		return listSQL;
	}

	public ParseObject parseSelectStatement(String query) throws JSQLParserException {
		tables = new HashMap<>();
		parentAlias = new HashMap<>();
		listCondition = new ArrayList<>();
		mappingKey = new HashMap<>();
		state = 1;
		String result[] = query.split(";");
		ParseObject parseCreate = new ParseObject();
		parseCreate.listTableSQL = new ArrayList<>();
		boolean isCreate = false;
		for (String querySingle : result) {
			Statement statement = CCJSqlParserUtil.parse(querySingle);
			if (statement instanceof Insert) {
				Insert insert = (Insert) statement;
				TableSQL tableSQL = TableSQL.builder().tableName(insert.getTable().getName()).build();
				tableSQL.columns = insert.getColumns().stream().map(item -> item.getName(false))
						.collect(Collectors.toSet());
				tableSQL.condition = new ArrayList<>();
				parseCreate.listTableSQL.add(tableSQL);
			}
			if (statement instanceof CreateTable) {
				isCreate = true;
				CreateTable createTable = (CreateTable) statement;
				TableSQL tableSQL = TableSQL.builder().tableName(createTable.getTable().getName())
						.alias(createTable.getTable().getName()).build();
				tableSQL.columns = createTable.getColumnDefinitions().stream().map(item -> item.getColumnName())
						.collect(Collectors.toSet());
				tableSQL.condition = new ArrayList<>();
				if (createTable.getIndexes() != null && !createTable.getIndexes().isEmpty()) {
					for (Index index : createTable.getIndexes()) {
						if (index instanceof ForeignKeyIndex) {
							ForeignKeyIndex foreignKeyIndex = (ForeignKeyIndex) index;
							mappingKey.put("KEY" + state,
		 							Arrays.asList(
											foreignKeyIndex.getTable().getName() + "."
													+ foreignKeyIndex.getReferencedColumnNames().get(0),
											tableSQL.tableName + "." + foreignKeyIndex.getColumnsNames().get(0)));
							System.out.println(Arrays.asList(
									foreignKeyIndex.getTable().getName() + "."
											+ foreignKeyIndex.getReferencedColumnNames().get(0),
									tableSQL.tableName + "." + foreignKeyIndex.getColumnsNames().get(0)));
							state++;
						}
					}
				}
				parseCreate.listTableSQL.add(tableSQL);
			}
			if (statement instanceof Select) {
				Select select = (Select) statement;
				processSelectBody(select.getSelectBody(), false, null);
				processPushCondition();
				parseCreate.listTableSQL = tables.entrySet().stream().map(table -> table.getValue())
						.collect(Collectors.toList());
				System.out.println("XXXXXXXXXXXXX");
				System.out.println(tables.toString());
				System.out.println(listCondition.toString());
				System.out.println(mappingKey.toString());
			}
		}
		parseCreate.mappingKey = mappingKey;
		if (isCreate) {
			mappingKey.entrySet().forEach(key -> {
				List<String> listCondition = key.getValue();
				boolean existTBL1 = parseCreate.listTableSQL.stream()
						.anyMatch(t -> t.tableName.equals(listCondition.get(0).split("\\.")[0]));
				boolean existTBL2 = parseCreate.listTableSQL.stream()
						.anyMatch(t -> t.tableName.equals(listCondition.get(1).split("\\.")[0]));
				if (existTBL1 && existTBL2) {
					key.getValue().forEach(tbl -> {
						String alias = tbl.split("\\.")[0];
						Optional<TableSQL> tblSQL = parseCreate.listTableSQL.stream()
								.filter(p -> p.tableName.equals(alias)).findFirst();
						if (tblSQL.isPresent()) {
							tblSQL.get().condition
									.add(Condition.builder().left(tbl).expression("=").right(key.getKey()).build());
						}
					});
				}
			});
		}
		return parseCreate;
	}

	public List<String> getListTableByStatement(String query) throws JSQLParserException {
		String result[] = query.split(";");
		List<String> listTable = new ArrayList<>();
		for (String querySingle : result) {
			Statement statement = CCJSqlParserUtil.parse(querySingle);
			if (statement instanceof Select) {
				Select selectStatement = (Select) statement;
				TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
				listTable.addAll(tablesNamesFinder.getTableList(selectStatement));
			}
			if (statement instanceof CreateTable) {
				CreateTable createTable = (CreateTable) statement;
				listTable.add(createTable.getTable().getName());
			}
			if (statement instanceof Insert) {
				Insert insert = (Insert) statement;
				listTable.add(insert.getTable().getName());
			}
		}
		return listTable;
	}

	private void processPushCondition() {
		listCondition.stream().forEach(condition -> {
			String alias = condition.left.split("\\.")[0];
			TableSQL tableSQL = tables.get(alias);
			if (tableSQL == null) {
				List<String> childAlias = parentAlias.get(alias);
				if (childAlias != null) {
					childAlias.forEach(child -> {
						TableSQL childTable = tables.get(child);
						if (childTable.getCondition() == null) {
							condition.setLeft(condition.left.replace(alias, childTable.alias));
							childTable.setCondition(new ArrayList<>(Arrays.asList(condition)));
						} else {
							condition.setLeft(condition.left.replace(alias, childTable.alias));
							childTable.condition.add(condition);
						}
						tables.put(child, childTable);
					});
				}
			} else {
				if (tableSQL.getCondition() == null) {

					tableSQL.setCondition(new ArrayList<>(Arrays.asList(condition)));
				} else {
					tableSQL.condition.add(condition);
				}
				tables.put(alias, tableSQL);
			}

		});
	}

	private void processSelectBody(SelectBody selectBody, boolean isNot, Alias alias) throws JSQLParserException {
		if (selectBody instanceof PlainSelect) {
			processSingle((PlainSelect) selectBody, isNot, alias);
		} else if (selectBody instanceof WithItem) {
		} else {
			SetOperationList operationList = (SetOperationList) selectBody;
			if (operationList.getSelects() != null && !operationList.getSelects().isEmpty()) {
				List<SelectBody> plainSelects = operationList.getSelects();
				for (SelectBody plainSelect : plainSelects) {
					processSelectBody(plainSelect, isNot, alias);
				}
			}
		}
	}

	private void processSingle(PlainSelect plainSelect, boolean isNot, Alias alias) throws JSQLParserException {
		processFromItem(plainSelect.getFromItem(), alias);
		List<Join> joins = plainSelect.getJoins();
		if (joins != null && !joins.isEmpty()) {
			joins.forEach(join -> {
				try {
					processJoin(join);
				} catch (JSQLParserException e) {
					e.printStackTrace();
				}
			});
		}
		if (plainSelect.getWhere() != null) {

			processExpression(plainSelect.getWhere(), isNot, plainSelect.getFromItem());
		}

	}

	private void processJoin(Join join) throws JSQLParserException {

		if (join.getRightItem() instanceof SubSelect) {
			SubSelect subSelect = (SubSelect) join.getRightItem();
			processExpression(join.getOnExpression(), false, join.getRightItem());
			processSelectBody(subSelect.getSelectBody(), false, join.getRightItem().getAlias());

		} else {
			processExpression(join.getOnExpression(), false, join.getRightItem());
			processTable(join.getRightItem());
		}
	}

	private void processAlias(Alias parent, FromItem current) {
		List<String> listAlias = parentAlias.get(parent.getName());
		if (listAlias == null) {
			listAlias = new ArrayList<>();
		}
		String currentAlias = current.getAlias() != null ? current.getAlias().getName() : current.toString();
		listAlias.add(currentAlias);
		parentAlias.put(parent.getName(), listAlias);
	}

	private void processTable(FromItem fromItem) {
		Table table = (Table) fromItem;
		String alias = table.getAlias() != null ? table.getAlias().getName() : table.getName();
		TableSQL tbl = tables.get(alias);
		if (tbl == null) {
			tbl = TableSQL.builder().tableName(table.getName()).alias(alias).condition(new ArrayList<>()).build();
		}
		tables.put(tbl.getAlias(), tbl);
	}

	private void processFromItem(FromItem fromItem, Alias alias) throws JSQLParserException {
		if (fromItem instanceof SubSelect) {
			SubSelect subSelect = (SubSelect) fromItem;
			processSelectBody(subSelect.getSelectBody(), false, alias != null ? alias : fromItem.getAlias());
		} else {
			processTable(fromItem);
			if (alias != null) {
				processAlias(alias, fromItem);
			}
		}
	}

	private void processFunction(Expression expression, String currentAlias) {
		BinaryExpression binaryExpression = (BinaryExpression) expression;
		Function function = (Function) binaryExpression.getRightExpression();
		Column column = (Column) binaryExpression.getLeftExpression();
		Condition condition = null;
		if (function.getName().equals("ALL")) {
			long maxValue = function.getParameters().getExpressions().stream().mapToLong(i -> {
				if (i instanceof LongValue) {
					return ((LongValue) i).getValue();
				}
				return 0;
			}).max().getAsLong();
			condition = Condition.builder().left(column.getFullyQualifiedName())
					.expression(binaryExpression.getStringExpression()).function(function.getName())
					.right(String.valueOf(maxValue)).build();
		}

		if (function.getName().equals("ANY")) {
			List<String> expressionList = function.getParameters().getExpressions().stream().map(i -> i.toString())
					.collect(Collectors.toList());
			condition = Condition.builder().left(column.getFullyQualifiedName())
					.expression(binaryExpression.getStringExpression()).function(function.getName())
					.right(expressionList.get(0)).build();
		}

		if (Objects.nonNull(condition)) {
			listCondition.add(condition);
		}
	}

	private void processComparisonExpression(Expression expression, String currentAlias) throws JSQLParserException {
		BinaryExpression binaryExpression = (BinaryExpression) expression;
		Column column = (Column) binaryExpression.getLeftExpression();
		Condition condition = null;
		if (binaryExpression.getRightExpression() instanceof AllComparisonExpression) {
			AllComparisonExpression allComparisonExpression = (AllComparisonExpression) binaryExpression
					.getRightExpression();
			allComparisonExpression.getSubSelect();
			processSelectBody(allComparisonExpression.getSubSelect().getSelectBody(), null);
		}
		if (binaryExpression.getRightExpression() instanceof AnyComparisonExpression) {
			AnyComparisonExpression anyComparisonExpression = (AnyComparisonExpression) binaryExpression
					.getRightExpression();
			processSelectBody(anyComparisonExpression.getSubSelect().getSelectBody(), null);
		}
	}

	private void processExpression(Expression expression, boolean isNot, FromItem alias) throws JSQLParserException {
		String currentAlias;
		if (alias instanceof Table) {
			currentAlias = alias.getAlias() != null ? alias.getAlias().getName() : alias.toString();
		} else {
			currentAlias = alias.getAlias().getName();
		}
		if (expression instanceof EqualsTo || expression instanceof GreaterThan
				|| expression instanceof GreaterThanEquals || expression instanceof MinorThan
				|| expression instanceof MinorThanEquals || expression instanceof NotEqualsTo) {
			BinaryExpression binary = (BinaryExpression) expression;
			if (binary.getLeftExpression() instanceof Column && binary.getRightExpression() instanceof Column) {
				String expresionLeft;
				String expresionRight;
				if (binary.getStringExpression().equals(GREATHER_OR_EQUAL)
						|| binary.getStringExpression().equals(LESS_OR_EQUAL)
						|| binary.getStringExpression().equals(GREATHER) || binary.getStringExpression().equals(LESS)) {
					expresionLeft = isNot ? reverseExpression.get(binary.getStringExpression())
							: binary.getStringExpression();
					expresionRight = isNot ? binary.getStringExpression()
							: reverseExpression.get(binary.getStringExpression());
				} else {
					expresionRight = isNot ? reverseExpression.get(binary.getStringExpression())
							: binary.getStringExpression();
					expresionLeft = isNot ? reverseExpression.get(binary.getStringExpression())
							: binary.getStringExpression();

				}
				Column leftColumn = (Column) binary.getLeftExpression();
				Column rightColumn = (Column) binary.getRightExpression();
				if (leftColumn.getTable() == null) {
					leftColumn.setTable(new Table(currentAlias));
				}
				if (rightColumn.getTable() == null) {
					rightColumn.setTable(new Table(currentAlias));
				}
				Condition conditionLeft = Condition.builder().left(leftColumn.toString()).expression(expresionLeft)
						.right("KEY" + state).build();
				listCondition.add(conditionLeft);
				Condition conditionRight = Condition.builder().left(rightColumn.toString()).expression(expresionRight)
						.right("KEY" + state).build();

				mappingKey.put("KEY" + state,
						new ArrayList<>(Arrays.asList(leftColumn.toString(), rightColumn.toString())));
				state++;
				listCondition.add(conditionRight);
			} else if (binary.getRightExpression() instanceof Column) {
				Condition condition = Condition.builder().left(binary.getRightExpression().toString())
						.expression(binary.getStringExpression()).right(binary.getLeftExpression().toString()).build();
				listCondition.add(condition);
			} else if (binary.getLeftExpression() instanceof Column
					&& binary.getRightExpression() instanceof SubSelect) {

				Column leftColumn = (Column) binary.getLeftExpression();
				if (leftColumn.getTable() == null) {
					leftColumn.setTable(new Table(currentAlias));
				}
				SelectBody selectBody = ((SubSelect) binary.getRightExpression()).getSelectBody();
				PlainSelect plainSelect = (PlainSelect) selectBody;
				List<String> listItems = new ArrayList<>();
				Table table = (Table) plainSelect.getFromItem();
				List<SelectItem> selectItems = plainSelect.getSelectItems();
				listItems.addAll(selectItems.stream().map(item -> {
					if (item.toString().contains(DOT)) {
						return item.toString();
					} else {
						return table.getAlias() != null ? table.getAlias().getName() + "." + item.toString()
								: table.getName() + "." + item.toString();
					}
				}).collect(Collectors.toList()));
				Condition condition = Condition.builder().left(leftColumn.toString())
						.expression(binary.getStringExpression()).right("KEY" + state).build();
				Condition conditionRight = Condition.builder().left(selectItems.get(0).toString())
						.expression(binary.getStringExpression()).right("KEY" + state).build();
				listCondition.add(condition);
				listCondition.add(conditionRight);
				mappingKey.put("KEY" + state,
						new ArrayList<>(Arrays.asList(leftColumn.toString(), selectItems.get(0).toString())));
				state++;
				processSelectBody(selectBody, isNot, null);
			} else if (binary.getRightExpression() instanceof Function) {
				processFunction(expression, currentAlias);
			} else if (binary.getRightExpression() instanceof AllComparisonExpression
					|| binary.getRightExpression() instanceof AnyComparisonExpression) {
				processComparisonExpression(binary, currentAlias);
			} else {
				if (binary.getLeftExpression() instanceof Column) {
					Column leftColumn = (Column) binary.getLeftExpression();
					if (leftColumn.getTable() == null) {
						leftColumn.setTable(new Table(currentAlias));
					}
					Condition condition = Condition.builder().left(leftColumn.toString())
							.expression(isNot ? reverseExpression.get(binary.getStringExpression())
									: binary.getStringExpression())
							.right(binary.getRightExpression().toString()).build();
					listCondition.add(condition);
				}
			}
		} else if (expression instanceof AndExpression) {
			AndExpression andExpression = (AndExpression) expression;
			processExpression(andExpression.getLeftExpression(), isNot, alias);
			processExpression(andExpression.getRightExpression(), isNot, alias);
		} else if (expression instanceof OrExpression) {
			OrExpression orExpression = (OrExpression) expression;
			processExpression(orExpression.getLeftExpression(), isNot, alias);
		} else if (expression instanceof NotExpression) {
			NotExpression notExpression = (NotExpression) expression;
			processExpression(notExpression.getExpression(), true, alias);
		} else if (expression instanceof InExpression) {
			InExpression inExpression = (InExpression) expression;
			if (inExpression.getLeftExpression() instanceof RowConstructor) {
				RowConstructor row = (RowConstructor) inExpression.getLeftExpression();
				row.getExprList().getExpressions().forEach(r -> {
					if (r instanceof Column) {

					}
				});
			} else if (inExpression.getLeftExpression() instanceof Column) {
				Column leftColumn = (Column) inExpression.getLeftExpression();
				if (leftColumn.getTable() == null) {
					leftColumn.setTable(new Table(currentAlias));
				}
				if (inExpression.getRightItemsList() instanceof SubSelect) {
					SubSelect subSelect = (SubSelect) inExpression.getRightItemsList();
					SelectBody selectBody = subSelect.getSelectBody();
					if (selectBody instanceof PlainSelect) {
						PlainSelect plainSelect = (PlainSelect) selectBody;
						List<String> listItems = new ArrayList<>();
						Table table = (Table) plainSelect.getFromItem();
						List<SelectItem> selectItems = plainSelect.getSelectItems();
						listItems.addAll(selectItems.stream().map(item -> {
							if (item.toString().contains(DOT)) {
								return item.toString();
							} else {
								return table.getAlias() != null ? table.getAlias().getName() + "." + item.toString()
										: table.getName() + "." + item.toString();
							}
						}).collect(Collectors.toList()));
						Condition conditionLeft = Condition.builder().left(leftColumn.toString()).right("KEY" + state)
								.expression(inExpression.isNot() ? EQUAL_NOT : EQUAL).build();

						listCondition.add(conditionLeft);
						Condition conditionRight = Condition.builder().left(listItems.get(0)).right("KEY" + state)
								.expression(inExpression.isNot() ? EQUAL_NOT : EQUAL).build();
						listCondition.add(conditionRight);

						mappingKey.put("KEY" + state,
								new ArrayList<>(Arrays.asList(leftColumn.toString(), listItems.get(0))));
						state++;
					} else {
						// operation subquery ... UNION
						SetOperationList operationList = (SetOperationList) selectBody;
						List<String> listItems = new ArrayList<>();
						operationList.getSelects().forEach(o -> {
							PlainSelect plainSelect = (PlainSelect) o;
							Table table = (Table) plainSelect.getFromItem();
							listItems.addAll(plainSelect.getSelectItems().stream().map(item -> {
								if (item.toString().contains(DOT)) {
									return item.toString();
								} else {
									return table.getName() + "." + item.toString();
								}
							}).collect(Collectors.toList()));
						});
						Condition condition = Condition.builder().left(leftColumn.toString())
								.expression(inExpression.isNot() ? NOT_IN : IN).listRight(listItems).build();
						listCondition.add(condition);
					}
					processSelectBody(selectBody, false, null);
				} else if (inExpression.getRightItemsList() instanceof ExpressionList) {
					ExpressionList expressionList = (ExpressionList) inExpression.getRightItemsList();
					Condition condition = Condition.builder().left(leftColumn.toString())
							.expression(inExpression.isNot() ? NOT_IN : IN).listRight(expressionList.getExpressions()
									.stream().map(value -> value.toString()).collect(Collectors.toList()))
							.build();
					listCondition.add(condition);
				}
			}

		} else if (expression instanceof ExistsExpression) {
			ExistsExpression existsExpression = (ExistsExpression) expression;
			if (existsExpression.getRightExpression() instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) existsExpression.getRightExpression();
				processSelectBody(subSelect.getSelectBody(), isNot, null);
			}
		} else if (expression instanceof Parenthesis) {
			Parenthesis parenthesis = (Parenthesis) expression;
			processExpression(parenthesis.getExpression(), isNot, alias);
		} else if (expression instanceof LikeExpression) {
			LikeExpression likeExpression = (LikeExpression) expression;
			if (likeExpression.getLeftExpression() instanceof Column
					&& likeExpression.getRightExpression() instanceof StringValue) {
				Column leftColumn = (Column) likeExpression.getLeftExpression();
				if (leftColumn.getTable() == null) {
					leftColumn.setTable(new Table(currentAlias));
				}
				Condition condition = Condition.builder().left(leftColumn.toString())
						.expression(likeExpression.isNot() ? "NOT LIKE" : "LIKE")
						.right(likeExpression.getRightExpression().toString()).build();
				listCondition.add(condition);
			}
		} else if (expression instanceof Between) {
			Between between = (Between) expression;
			Expression expressionStart = between.getBetweenExpressionStart();
			Expression expressionEnd = between.getBetweenExpressionEnd();
			Expression expressionLeft = between.getLeftExpression();
			Column leftColumn = (Column) expressionLeft;
			if (leftColumn.getTable() == null) {
				leftColumn.setTable(new Table(currentAlias));
			}
			if (expressionStart instanceof Function) {
				Function leftFunction = (Function) expressionStart;
				Condition condition = Condition.builder().left(leftColumn.toString())
						.right(leftFunction.getParameters().getExpressions().get(0).toString())
						.expression(isNot ? reverseExpression.get(GREATHER_OR_EQUAL) : GREATHER_OR_EQUAL)
						.function(leftFunction.getName()).build();
				listCondition.add(condition);
			} else if (expressionStart instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) expressionStart;
				processSelectBody(subSelect.getSelectBody(), false, null);
				List<String> listItems = new ArrayList<>();
				List<SelectItem> selectItems = ((PlainSelect) subSelect.getSelectBody()).getSelectItems();

				listItems.addAll(selectItems.stream().map(item -> {
					return item.toString();
				}).collect(Collectors.toList()));

				Condition condition = Condition.builder().left(expressionLeft.toString())
						.expression(isNot ? reverseExpression.get(GREATHER_OR_EQUAL) : GREATHER_OR_EQUAL)
						.right("KEY" + state).build();

				String aliasRight = ((PlainSelect) subSelect.getSelectBody()).getFromItem().getAlias() != null
						? ((PlainSelect) subSelect.getSelectBody()).getFromItem().getAlias().getName()
						: ((PlainSelect) subSelect.getSelectBody()).getFromItem().toString();

				Condition conditionRight = Condition.builder().left(aliasRight + DOT + listItems.get(0))
						.expression(isNot ? reverseExpression.get(LESS_OR_EQUAL) : LESS_OR_EQUAL).right("KEY" + state)
						.build();
				mappingKey.put("KEY" + state,
						new ArrayList<>(Arrays.asList(expressionLeft.toString(), aliasRight + DOT + listItems.get(0))));
				listCondition.add(conditionRight);
				listCondition.add(condition);
				state++;
			} else {

				Condition condition = Condition.builder().left(between.getLeftExpression().toString())
						.expression(isNot ? reverseExpression.get(GREATHER_OR_EQUAL) : GREATHER_OR_EQUAL)
						.right(expressionStart.toString()).build();
				listCondition.add(condition);
			}

			if (expressionEnd instanceof Function) {
				Function functionRight = (Function) expressionEnd;
				Condition condition = Condition.builder().left(expressionLeft.toString())
						.right(functionRight.getParameters().getExpressions().get(0).toString())
						.expression(isNot ? reverseExpression.get(LESS_OR_EQUAL) : LESS_OR_EQUAL)
						.function(functionRight.getName()).build();
				listCondition.add(condition);
			} else if (expressionEnd instanceof SubSelect) {
				SubSelect subSelect = (SubSelect) expressionEnd;
				// is item select
				processSelectBody(subSelect.getSelectBody(), false, null);
				List<String> listItems = new ArrayList<>();
				List<SelectItem> selectItems = ((PlainSelect) subSelect.getSelectBody()).getSelectItems();
				listItems.addAll(selectItems.stream().map(item -> item.toString()).collect(Collectors.toList()));
				Condition condition = Condition.builder().left(expressionLeft.toString())
						.expression(isNot ? reverseExpression.get(LESS_OR_EQUAL) : LESS_OR_EQUAL).right("KEY" + state)
						.build();
				listCondition.add(condition);

				String aliasRight = ((PlainSelect) subSelect.getSelectBody()).getFromItem().getAlias() != null
						? ((PlainSelect) subSelect.getSelectBody()).getFromItem().getAlias().getName()
						: ((PlainSelect) subSelect.getSelectBody()).getFromItem().toString();

				Condition conditionRight = Condition.builder().left(aliasRight + DOT + listItems.get(0))
						.expression(isNot ? reverseExpression.get(GREATHER_OR_EQUAL) : GREATHER_OR_EQUAL)
						.right("KEY" + state).build();

				mappingKey.put("KEY" + state,
						new ArrayList<>(Arrays.asList(expressionLeft.toString(), aliasRight + DOT + listItems.get(0))));
				listCondition.add(conditionRight);
				state++;
			} else {
				Condition condition = Condition.builder().left(between.getLeftExpression().toString())
						.expression(isNot ? reverseExpression.get(LESS_OR_EQUAL) : LESS_OR_EQUAL)
						.right(expressionEnd.toString()).build();
				listCondition.add(condition);
			}
		}

	}
}
