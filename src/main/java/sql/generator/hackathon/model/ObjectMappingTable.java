package sql.generator.hackathon.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sql.generator.hackathon.model.createdata.ColumnCondition;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectMappingTable {

	private String columnName;
	private List<ColumnCondition> columnsCondition;
}
