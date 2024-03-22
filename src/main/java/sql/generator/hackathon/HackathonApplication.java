package sql.generator.hackathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import net.sf.jsqlparser.JSQLParserException;

@SpringBootApplication
public class HackathonApplication extends SpringBootServletInitializer {

	public static void main(String[] args) throws JSQLParserException {
		SpringApplication.run(HackathonApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(HackathonApplication.class);
	}
}
