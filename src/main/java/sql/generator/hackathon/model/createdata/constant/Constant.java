package sql.generator.hackathon.model.createdata.constant;

import java.util.HashMap;
import java.util.Map;

public class Constant {
	public static final Map<String, Integer> priorityOperators = new HashMap<>();
	static {
		priorityOperators.put("=", 1);
		priorityOperators.put("IN", 2);
		priorityOperators.put("LIKE", 3);
		priorityOperators.put(">=", 4);
		priorityOperators.put("<=", 5);
		priorityOperators.put(">", 6);
		priorityOperators.put("<", 7);
		priorityOperators.put("NOT IN", 8);
		priorityOperators.put("!=", 9);
		priorityOperators.put("<>", 10);
	}
	
	public static final String EXPRESSION_EQUALS = "=";
	public static final String EXPRESSION_IN = "IN";
	public static final String EXPRESSION_LIKE = "LIKE";
	public static final String EXPRESSION_GREATER_EQUALS = ">=";
	public static final String EXPRESSION_LESS_EQUALS = "<=";
	public static final String EXPRESSION_GREATER = ">";
	public static final String EXPRESSION_LESS = "<";
	public static final String EXPRESSION_NOT_IN = "NOT IN";
	public static final String EXPRESSION_DIFF_1 = "!=";
	public static final String EXPRESSION_DIFF_2 = "<>";
	
	// Character
	public static final char DEFAULT_CHAR = 'A';
	public static final char CHAR_Z = 'Z';
	
	// Integer
	public static final int DEFAULT_LENGTH = 9;
	public static final int DEFAULT_NUMBER = 1;
	
	public static final int LIMIT_GEN_VALUE = 100;
	
	public static final int LENGTH_UNICODE_CHARACTER = 26;
	
	public static final int DEFAULT_LENGTH_TYPE_CHAR = 255; 
	
	
	// String regex
	public static final String STR_DOT = ".";
	public static final String STR_UNDERLINE = "_";
	
	public static final String STR_APOSTROPHE = "'";
	
	public static final String STR_TYPE_CHAR = "char";
	public static final String STR_TYPE_NUMBER = "number";
	public static final String STR_TYPE_DATE = "date";
	
	public static final String NO_CONNECTION = "0";
	
	public static final String STR_NO_CONNECTION = "No Database";
	
	public static final String STR_KEYS = "KEY";
	
	public static final String KEY_MARK_COLOR = "MARK_COLOR";

	public static final String DEFAULT_NUM_MARK_COLOR = "47";
	
	// DATE TIME
	public static final String DEFAULT_FORMAT_DATE = "yyyy-MM-dd";
	
	
	public static final char CHAR_APOSTROPHE = '\'';
}
