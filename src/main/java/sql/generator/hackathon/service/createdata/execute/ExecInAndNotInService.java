package sql.generator.hackathon.service.createdata.execute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

@Service
public class ExecInAndNotInService {

	/**
	 * Insert values to current list, confirm not duplicate values
	 * @param currentValues
	 * @param valuesAdd
	 * @return
	 */
	public List<String> processExpressionIn(List<String> currentValues, List<String> valuesAdd) {
		if (currentValues == null || currentValues.isEmpty()) {
			return valuesAdd;
		}
		if (valuesAdd == null || valuesAdd.isEmpty()) {
			return currentValues;
		}
		Set<String> res = new HashSet<>();
		res.addAll(currentValues);
		res.addAll(valuesAdd);
		return res.stream().collect(Collectors.toList());
	}
	
	
	
	
	/**
	 * Execute get values expect values not in
	 * @param valuesIn
	 * @param valuesNotIn
	 * @return
	 */
	public List<String> processNotIn(List<String> currentValues, List<String> valuesNotIn) {
		if (currentValues == null || currentValues.isEmpty()) {
			return new ArrayList<String>();
		}
		
		if (valuesNotIn == null) {
			valuesNotIn = Collections.<String>emptyList();
		}
		return processCalcLastValue2(currentValues, valuesNotIn);
	}
	
	public List<String> processNotIn(List<String> currentValues, String valueNotIn) {
		if (currentValues == null || currentValues.isEmpty()) {
			return new ArrayList<String>();
		}
		List<String> valuesNotIn = Arrays.asList(valueNotIn);
		return processCalcLastValue2(currentValues, valuesNotIn);
	}
	
	/**
	 * Execute get values expect values not in
	 * @param valuesIn
	 * @param valuesNotIn
	 * @return
	 */
	private List<String> processCalcLastValue2(List<String> currentValues, List<String> valuesNotIn) {
		return currentValues.stream().filter(x -> !valuesNotIn.contains(x))
				.collect(Collectors.toList());
	}
}
