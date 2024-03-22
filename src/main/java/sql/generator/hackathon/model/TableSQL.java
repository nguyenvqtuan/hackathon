package sql.generator.hackathon.model;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSQL {
	public String tableName;
	public String alias;
	public List<Condition> condition;
	public Set<String> columns;
}