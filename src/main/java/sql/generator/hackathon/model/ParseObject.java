package sql.generator.hackathon.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParseObject {
	public List<TableSQL> listTableSQL;
	public Map<String, List<String>> mappingKey;
}
