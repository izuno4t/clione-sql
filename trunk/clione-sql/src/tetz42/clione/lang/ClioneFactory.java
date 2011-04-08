package tetz42.clione.lang;

import static tetz42.clione.util.ClioneUtil.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClioneFactory {

	private static final Pattern ptn = Pattern
			.compile("([$@&?#:%]?)(!?)([a-zA-Z0-9\\.\\-_]*)(\\s*)");

	public static ClioneFactory get(String resourceInfo) {
		return new ClioneFactory(resourceInfo);
	}

	private final String resourceInfo;
	
	private ClioneFactory(String resourceInfo) {
		this.resourceInfo = resourceInfo;
	}

	public Clione parse(String src) {
		src = src.trim();
		return parse(src, ptn.matcher(src));
	}

	private Clione parse(String src, Matcher m) {
		if (!m.find())
			return null;
		Clione clione = gen(src, m, m.group(1), m.group(2), m.group(3));
		if (clione == null)
			return null;
		clione.setResourceInfo(resourceInfo);
		if(clione.isTerminated())
			return clione;
		clione.setNext(parse(src, m));
		return clione;
	}

	private Clione gen(String src, Matcher m, String func, String not,
			String key) {
		if (isAllEmpty(func, not, key))
			return null;
		if (isAllEmpty(func, not))
			return new Param(key);
		if (isNotEmpty(func)) {
			if (func.equals(":"))
				return new Literal(src.substring(m.end(1)), true);
			if (func.equals("$"))
				return new LineParam(key, isNotEmpty(not));
			if (func.equals("@"))
				return new RequireParam(key);
			if (func.equals("?"))
				return new DefaultParam(key);
			if (func.equals("#"))
				return new PartCond(key, isNotEmpty(not));
			if (func.equals("&"))
				return new LineCond(key, isNotEmpty(not));
			if(func.equals("%"))
				return null; // TODO implementation
		}
		// TODO throw exception if unsupported grammer is found.
		return null;
	}

}