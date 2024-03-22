package sql.generator.hackathon.service.createdata.execute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import sql.generator.hackathon.model.createdata.constant.Constant;

@Service
public class ExecLikeService {
	
	private List<String> operators = Arrays.asList("_", "%");
	
	public List<String> processLike(String value) {
		if (value == null) {
			return new ArrayList<>(); 
		}
		
		// Find other character in operators has define.
		long c = operators.stream().filter(x -> value.contains(x)).count();
		if (c <= 0) {
			System.out.println("Error operators in LIKE condition");
			return new ArrayList<>();
		}
		List<String> res = new ArrayList<>();
		char[] chrArr = value.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (char chr : chrArr) {
			
			switch (chr) {
			case '%':
				// TODO
				break;
			case '_':
				res = processCreateValueForUnderLine(res, sb.toString());
				break;
			default:
				sb.append(chr);
				break;
			}
		}
		return res.isEmpty() ? Arrays.asList(sb.toString()) : res;
	}
	
	/**
	 * 
	 * @param currentValue
	 * @param c
	 */
	private List<String> processCreateValueForUnderLine(List<String> currentValue, String prevValue) {
		List<String> res = new ArrayList<>();
		if (currentValue.isEmpty()) {
			res.addAll(processCreate26Char(prevValue));
			return res;
		}
		currentValue.stream().forEach(x -> {
			res.addAll(processCreate26Char(x));
		});
		return res;
	}
	
	private List<String> processCreate26Char(String value) {
		List<String> res = new ArrayList<>();
		for (int i = 0; i < Constant.LENGTH_UNICODE_CHARACTER; ++i) {
			res.add(value + String.valueOf((char) (Constant.DEFAULT_CHAR + i)));
		}
		return res;
	}
}
