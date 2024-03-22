package sql.generator.hackathon.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Condition {
	public String left;
	public String right;
	public String expression;
	public List<String> listRight;
	public String function;
	
	public Condition(String left, String expression, String right) {
		this.left = left;
		this.expression = expression;
		this.right = right;
	}
}
