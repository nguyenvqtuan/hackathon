package sql.generator.hackathon.service;

import java.util.Calendar;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.github.javafaker.Faker;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;

@Service
public class ServerFaker {
	public String getDataByColumn(String column, String dataType, String locale) {
		FakeValuesService fakeValuesService = new FakeValuesService(new Locale(locale), new RandomService());
		Faker faker = new Faker(new Locale(locale));
		if (!dataType.isEmpty()) {
			Calendar end = Calendar.getInstance();
			end.add(Calendar.YEAR, 1);
			return faker.date().between(Calendar.getInstance().getTime(), end.getTime()).toString();
		}
		if (column.toLowerCase().contains("birthday")) {
			return faker.date().birthday().toString();
		}
		if (column.toLowerCase().contains("date")) {
			Calendar end = Calendar.getInstance();
			end.add(Calendar.YEAR, 1);
			return faker.date().between(Calendar.getInstance().getTime(), end.getTime()).toString();
		}
		if (column.toLowerCase().contains("address")) {
			return faker.address().cityName() + faker.address().country();
		}
		if (column.toLowerCase().contains("city")) {
			return faker.address().cityName();
		}
		if (column.toLowerCase().contains("street")) {
			return faker.address().streetName();
		}
		if (column.toLowerCase().contains("country")) {
			return faker.address().country();
		}
		if (column.toLowerCase().contains("zip")) {
			return faker.address().zipCode();
		}
		if (column.toLowerCase().contains("phone")) {
			return faker.phoneNumber().phoneNumber();
		}
		if (column.toLowerCase().contains("name")) {
			return faker.name().fullName();
		}
		if (column.toLowerCase().contains("email")) {
			return fakeValuesService.bothify("????##@gmail.com");
		}
		if (column.toLowerCase().contains("id")) {
			return fakeValuesService.regexify("[0-9]{2}");
		}
		if (column.toLowerCase().contains("color")) {
			return faker.color().name();
		}

		if (column.toLowerCase().contains("company")) {
			return faker.company().name();
		}

		if (column.toLowerCase().contains("code")) {
			return faker.code().isbn10();
		}
		return faker.name().fullName();
	}
}
