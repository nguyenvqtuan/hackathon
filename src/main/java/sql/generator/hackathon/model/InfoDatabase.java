package sql.generator.hackathon.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoDatabase {
	public String url;
	public String schema;
	public String user;
	public String password;
	public String type;
	
}
