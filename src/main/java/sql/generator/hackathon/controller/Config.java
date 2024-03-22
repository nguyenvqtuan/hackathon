package sql.generator.hackathon.controller;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

@Configuration
@EnableAutoConfiguration(exclude = { H2ConsoleAutoConfiguration.class, DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class, JdbcTemplateAutoConfiguration.class })
public class Config {
	@Bean
	@Scope("prototype")
	public DataSource dataSource(String driver, String url, String name, String pass) {
		return DataSourceBuilder.create().type(SimpleDriverDataSource.class).driverClassName(driver).url(url)
				.username(name).password(pass).build();
	}
}
