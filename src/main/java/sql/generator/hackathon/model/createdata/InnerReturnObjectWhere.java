package sql.generator.hackathon.model.createdata;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InnerReturnObjectWhere {
	private List<String> validValueForColumn;
	private List<String> inValidValueForColumn;
	private String lastValue;
	private String markColor;
}
