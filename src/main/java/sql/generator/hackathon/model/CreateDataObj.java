package sql.generator.hackathon.model;

import java.util.ArrayList;
import java.util.List;

public class CreateDataObj {

	// Calculator valid values in Where.
	// Value = List<Cond> => Cond
	private List<Cond> validValuesForColumn;
	
	// List data use operator (NOT IN, !=, <>)
	private List<String> valueInValidOfColumn;
	private String lastEndValidValue;
	
	// markColor for column
	private String markColor;
	
	// Save list operator of current table
	private List<String> listOperator;
	
	// Save list value in where
	private List<String> listValue;
	
	public CreateDataObj() {
		validValuesForColumn = new ArrayList<>();
		valueInValidOfColumn = new ArrayList<>();
		lastEndValidValue = "";
		markColor = "";
		listOperator = new ArrayList<>();
		listValue = new ArrayList<>();
	}

	public List<Cond> getValidValuesForColumn() {
		return validValuesForColumn;
	}

	public void setValidValuesForColumn(List<Cond> validValuesForColumn) {
		this.validValuesForColumn = validValuesForColumn;
	}

	public List<String> getValueInValidOfColumn() {
		return valueInValidOfColumn;
	}

	public void setValueInValidOfColumn(List<String> valueInValidOfColumn) {
		this.valueInValidOfColumn = valueInValidOfColumn;
	}

	public String getLastEndValidValue() {
		return lastEndValidValue;
	}

	public void setLastEndValidValue(String lastEndValidValue) {
		this.lastEndValidValue = lastEndValidValue;
	}

	public String getMarkColor() {
		return markColor;
	}

	public void setMarkColor(String markColor) {
		this.markColor = markColor;
	}

	public List<String> getListOperator() {
		return listOperator;
	}

	public void setListOperator(List<String> listOperator) {
		this.listOperator = listOperator;
	}

	public List<String> getListValue() {
		return listValue;
	}

	public void setListValue(List<String> listValue) {
		this.listValue = listValue;
	}
}
