package sql.generator.hackathon.service.createdata.execute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.sf.jsqlparser.JSQLParserException;
import sql.generator.hackathon.model.ColumnInfo;
import sql.generator.hackathon.model.ParseObject;
import sql.generator.hackathon.model.TableSQL;
import sql.generator.hackathon.model.createdata.constant.Constant;
import sql.generator.hackathon.service.ServiceParse;
import sql.generator.hackathon.service.createdata.CommonService;

@Service
public class ExecClientService {

	@Autowired
	private ServiceParse parseService;
	
	private ParseObject parseObject;
	
	private CommonService commonService;
	
	private Map<String, TableSQL> fullTableInfo;
	
	public void init(CommonService commonService, ParseObject parseObject) throws JSQLParserException {
		this.parseObject = parseObject;
		this.commonService = commonService;
		fullTableInfo = new HashMap<>();
		fullTableInfo = parseService.getColumnInfo(commonService.objCommon.getObjectGenate().getQueryInput());
	}
	
	public void setClientData(String tableName, int idxRow, 
			List<ColumnInfo> curListColumn, Map<String, List<List<ColumnInfo>>> clientData) {
		// Set value from client!
		List<ColumnInfo> client = new ArrayList<>();
		if (!clientData.isEmpty() && clientData.get(tableName) != null) {
			if (idxRow >= clientData.get(tableName).size()) {
				client = clientData.get(tableName).get(clientData.get(tableName).size() - 1);
			} else {
				client = clientData.get(tableName).get(idxRow);
			}
		}
		
		if (!client.isEmpty()) {
			for (ColumnInfo colInfo : curListColumn) {
				for (ColumnInfo c : client) {
					if (c.val != null && !c.val.equals("null") 
							&& colInfo.getName().equals(c.getName()) && colInfo.val.isEmpty()) {
						colInfo.val = c.val;
					}
				}
			}
		}
		
		if (commonService.objCommon.getObjectGenate().getInfoDatabase().getType().equalsIgnoreCase(Constant.STR_NO_CONNECTION)) {
			Set<ColumnInfo> clientTableData = new HashSet<>();
			for (ColumnInfo c : client) {
				boolean flg = true;
				for (ColumnInfo colInfo : curListColumn) {
					if (c.getName() != null && c.getName().equals(colInfo.getName())) {
						flg = false;
						break;
					}
				}
				if (flg) {
					clientTableData.add(c);
				}
			}
			for (ColumnInfo colInfo : clientTableData) {
				curListColumn.add(colInfo);
			}
		}
	}
	
	/**
	 * Add column get from select
	 */
	public void addColumnGetFromSelect(List<ColumnInfo> columns, String aliasName) {
		if (!commonService.objCommon.getObjectGenate().getInfoDatabase().getType().equalsIgnoreCase(Constant.STR_NO_CONNECTION)) {
			return;
		}

		Set<String> columnInSelect;
		if (fullTableInfo.get(aliasName) != null) {
			columnInSelect = fullTableInfo.get(aliasName).getColumns();
		} else {
			return;
		}
		
		if (columnInSelect == null) {
			return;
		}
		 
		Set<String> columnNeedAdd = new HashSet<>();
		for (String colName : columnInSelect) {
			boolean flgAdd = true;
			for (ColumnInfo colInfo : columns) {
				if (colName.equals(colInfo.getName())) {
					flgAdd = false;
					break;
				}
			}
			if (flgAdd) {
				columnNeedAdd.add(colName);
			}
		}
		
		for (String colName : columnNeedAdd) {
			ColumnInfo colInfo = new ColumnInfo(colName, "", Constant.STR_TYPE_CHAR, 
					String.valueOf(Constant.DEFAULT_LENGTH_TYPE_CHAR));
			columns.add(colInfo);
		}
	}
}
