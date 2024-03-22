package sql.generator.hackathon.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectGenate {
	public InfoDatabase infoDatabase;
	public String queryInput;
	public List<ObjectDataPicker> dataPicker;
	public String typeExport;
	public int row;
	public String language;
}
