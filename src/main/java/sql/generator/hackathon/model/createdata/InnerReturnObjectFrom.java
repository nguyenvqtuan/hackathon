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
public class InnerReturnObjectFrom {
	private String lastValue;
	private String markColor; 
	private List<String> listValidValue;
}
