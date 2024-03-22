package sql.generator.hackathon.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectDataPicker {
	public String tableName;
	public List<List<ColumnInfo>> listColumnInfo;
}
