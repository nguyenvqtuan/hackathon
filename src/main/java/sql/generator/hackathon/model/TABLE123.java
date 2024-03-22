package sql.generator.hackathon.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
public class TABLE123 {
	@Id
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public TABLE123(Long id) {
		this.id = id;
	}

	public TABLE123() {
	}
	
}
