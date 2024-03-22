package sql.generator.hackathon.model.createdata;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sql.generator.hackathon.model.ObjectMappingTable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectCreateData {
	// Key = tableName-aliasName
	private Map<String, List<ObjectMappingTable>> mappingTables;
}
