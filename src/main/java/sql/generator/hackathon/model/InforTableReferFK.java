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
public class InforTableReferFK {
	boolean hasExist;
	String tableReferFKName;
	List<ColumnInfo> columnInfoLst;
}
