package sql.generator.hackathon.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ObjectCommonCreate {
	private ObjectGenate objectGenate;
	private List<String> listTableName;
	private Map<String, List<ColumnInfo>> tableInfo;
	private Set<String> hasReFormat;
	private Map<String, List<String>> listTableAlias;
}
