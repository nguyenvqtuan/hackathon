package sql.generator.hackathon.model.createdata;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionObject {

	private List<String> listValidValue;
	private String lastValue;
	private List<String> listInValidValue;
}
