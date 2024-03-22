package sql.generator.hackathon.model.createdata;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InfoMappingTableColumnObject {

	private List<String> listExpression;
	private List<String> listValue;
}
