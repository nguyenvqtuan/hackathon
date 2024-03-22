package sql.generator.hackathon.model;

public class Cond{
	public String operator = "";
	public String value = "";
	public String operatorRight = "";
	public String rightValue = "";
	
	public Cond() {
		
	}
	
	public Cond(String operator, String value) {
		this.operator = operator;
		this.value = value;
	}
	
	public Cond(String operator, String value, String operatorRight, String rightValue) {
		this.operator = operator;
		this.value = value;
		this.operatorRight = operatorRight;
		this.rightValue = rightValue;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Cond)) {
			return false;
		}
		Cond obj = (Cond) o;
		return this.value.equals(obj.value) && this.operator.equals(obj.operator);
	}
	
	@Override
	public int hashCode() {
		return operator.hashCode() + value.hashCode() * 31;
	}
}
