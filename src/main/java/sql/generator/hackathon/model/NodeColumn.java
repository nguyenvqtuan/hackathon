package sql.generator.hackathon.model;

import java.util.Map;

public class NodeColumn {

	public String tableColumnName;
	public String val;
	public int index;
	public Cond mapping;
	public Map<String, String> valCompositeKey;
	
	public NodeColumn(String tableColumnName, String val, int index, Map<String, String> valCompositeKey) {
		this.tableColumnName = tableColumnName;
		this.val = val;
		this.index = index;
		this.valCompositeKey = valCompositeKey;
	}
	
	public NodeColumn(String tableColumnName, String val, int index, Cond mapping, Map<String, String> valCompositeKey) {
		this.tableColumnName = tableColumnName;
		this.val = val;
		this.index = index;
		this.mapping = mapping;
		this.valCompositeKey = valCompositeKey;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (! (o instanceof NodeColumn)) {
			return false;
		}
		
		NodeColumn node = (NodeColumn) o;
		return node.tableColumnName.equals(this.tableColumnName)  
				&& node.val.equals(this.val);
	}
	
	@Override
	public int hashCode() {
		int k = 31;
		return (this.tableColumnName.hashCode() + this.val.hashCode()) * k; 
	}
}
