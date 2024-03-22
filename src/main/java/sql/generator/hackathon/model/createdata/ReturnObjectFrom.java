package sql.generator.hackathon.model.createdata;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnObjectFrom {
	// Key = tableName.aliasName.columnName
	private Map<String, InnerReturnObjectFrom> mappingTableAliasColumn;
}
