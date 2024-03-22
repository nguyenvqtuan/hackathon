package sql.generator.hackathon.model;

public class ColumnInfo {
	public String tableAlias;
	public String name;
	public String val;
	public String typeName;
	public String typeValue;
	public boolean isNull;
	public boolean isPrimarykey;
	public boolean isForeignKey;
	public boolean unique;
	
	public String color = "";
	
	public ColumnInfo() {
	}
	
	public ColumnInfo(String name, String val) {
		this.name = name;
		this.val = val;
	}
	
	public ColumnInfo(String name, String val, String typeName) {
		this.name = name;
		this.val = val;
		this.typeName = typeName;
	}

	public ColumnInfo(String name, String val, String typeName, String typeValue) {
		this.name = name;
		this.val = val;
		this.typeName = typeName;
		this.typeValue = typeValue;
	}
	
	public ColumnInfo(String name, String val, String typeName, String typeValue, boolean isNull, boolean isPrimarykey,
			boolean isForeignKey, boolean unique) {
		this.name = name;
		this.val = val;
		this.typeName = typeName;
		this.typeValue = typeValue;
		this.isNull = isNull;
		this.isPrimarykey = isPrimarykey;
		this.isForeignKey = isForeignKey;
		this.unique = unique;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public String getTypeValue() {
		return typeValue;
	}
	public void setTypeValue(String typeValue) {
		this.typeValue = typeValue;
	}
	public Boolean getIsNull() {
		return isNull;
	}
	public void setIsNull(Boolean isNull) {
		this.isNull = isNull;
	}
	public Boolean getIsPrimarykey() {
		return isPrimarykey;
	}
	public void setIsPrimarykey(Boolean isPrimarykey) {
		this.isPrimarykey = isPrimarykey;
	}
	public Boolean getIsForeignKey() {
		return isForeignKey;
	}
	public void setIsForeignKey(Boolean isForeignKey) {
		this.isForeignKey = isForeignKey;
	}
	public Boolean getUnique() {
		return unique;
	}
	public void setUnique(Boolean unique) {
		this.unique = unique;
	}
	
	public Boolean isKey() {
		return isPrimarykey || isForeignKey;
	}

	public String getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}
	
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
	
	public String getTableAlias() {
		return tableAlias;
	}

	public void setTableAlias(String tableAlias) {
		this.tableAlias = tableAlias;
	}
}
