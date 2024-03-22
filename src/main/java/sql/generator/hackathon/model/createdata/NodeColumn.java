package sql.generator.hackathon.model.createdata;

import java.util.Map;

import sql.generator.hackathon.model.Cond;

public class NodeColumn {

	public String tableAliasColumnName;
	public String val;
	public int index;
	public Cond mapping;
	public Map<String, String> valCompositeKey;
	
	public NodeColumn(String tableAliasColumnName, String val, int index, Map<String, String> valCompositeKey) {
		this.tableAliasColumnName = tableAliasColumnName;
		this.val = val;
		this.index = index;
		this.valCompositeKey = valCompositeKey;
	}
	
	public NodeColumn(String tableAliasColumnName, String val, int index, Cond mapping, Map<String, String> valCompositeKey) {
		this.tableAliasColumnName = tableAliasColumnName;
		this.val = val;
		this.index = index;
		this.mapping = mapping;
		this.valCompositeKey = valCompositeKey;
	}
	
	
	public String getTableAliasColumnName() {
		return tableAliasColumnName;
	}

	public void setTableAliasColumnName(String tableAliasColumnName) {
		this.tableAliasColumnName = tableAliasColumnName;
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Cond getMapping() {
		return mapping;
	}

	public void setMapping(Cond mapping) {
		this.mapping = mapping;
	}

	public Map<String, String> getValCompositeKey() {
		return valCompositeKey;
	}

	public void setValCompositeKey(Map<String, String> valCompositeKey) {
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
		return node.tableAliasColumnName.equals(this.tableAliasColumnName)  
				&& node.val.equals(this.val);
	}
	
	@Override
	public int hashCode() {
		int k = 31;
		return (this.tableAliasColumnName.hashCode() + this.val.hashCode()) * k; 
	}
}
