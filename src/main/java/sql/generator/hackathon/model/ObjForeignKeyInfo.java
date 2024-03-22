package sql.generator.hackathon.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjForeignKeyInfo {
	public String tableName;
	public String columnName;
	public String referencedTableName;
	public String referencedColumnName;
}
