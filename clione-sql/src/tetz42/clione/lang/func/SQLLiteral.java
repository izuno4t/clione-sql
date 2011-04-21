package tetz42.clione.lang.func;

import java.io.StringReader;
import java.util.List;

import tetz42.clione.gen.SQLGenerator;
import tetz42.clione.lang.Instruction;
import tetz42.clione.node.LineNode;
import tetz42.clione.parsar.SQLParser;
import tetz42.clione.util.ParamMap;

public class SQLLiteral extends ClioneFunction {

	private final String literal;
	private final List<LineNode> nodes;

	public SQLLiteral(String literal) {
		// System.out.println("\tliteral=" + literal);
		this.literal = literal;
		StringReader reader = new StringReader(literal);
		this.nodes = new SQLParser(resourceInfo).parse(reader);
	}

	@Override
	public Instruction perform(ParamMap paramMap) {
		Instruction instruction = getInstruction(paramMap);
		SQLGenerator sqlGenerator = new SQLGenerator(null);
		instruction.replacement = sqlGenerator.genSql(paramMap, this.nodes);
		if (sqlGenerator.params != null && sqlGenerator.params.size() != 0) {
			instruction.params.addAll(sqlGenerator.params);
		}
		return instruction;
	}

	@Override
	public String getSrc() {
		return "\"" + literal + "\"";
	}
}
