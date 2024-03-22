package sql.generator.hackathon.model;

import java.util.ArrayList;
import java.util.List;

public class CommonCreateObj {

	private int type; // 0: No connection, 1: connection db
	private String schema;
	private List<String> listTable;
	
	public CommonCreateObj() {
	}
	
	public void init(int type) {
		this.type = type;
		this.schema = "";
		this.listTable = new ArrayList<>();
	}
	
	public void init(int type, String schema, List<String> listTable) {
		this.type = type;
		this.schema = schema;
		this.listTable = listTable;
	}
	
	public int getType() {
		return type;
	}
	
	public String getSchema() {
		return schema;
	}
	
	public List<String> getListTable() {
		return listTable;
	}
}
