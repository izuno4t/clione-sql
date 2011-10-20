package tetz42.clione.node;

import static tetz42.clione.lang.LangUtil.*;

import java.util.ArrayList;

import tetz42.clione.lang.Instruction;
import tetz42.clione.util.ClioneUtil;
import tetz42.clione.util.ParamMap;

public class ConditionPlaceHolder extends PlaceHolder implements IPlaceHolder {

	private static final int IN_MAX = 1000;

	private final Node node;
	private final String operator;
	private final boolean isPositive;

	public ConditionPlaceHolder(Node node, String comment, boolean isPositive,
			String operator, INode valueInBack) {
		super(comment, valueInBack);
		this.node = node;
		this.operator = operator;
		this.isPositive = isPositive;
	}

	@Override
	public Instruction perform(ParamMap paramMap) {
		Instruction inst = super.perform(paramMap);
		if (inst.isNodeDisposed || inst.doNothing)
			return inst;
		Instruction nodeInst = node.perform(paramMap);
		if (nodeInst.isNodeDisposed || nodeInst.doNothing)
			return nodeInst;

		if (inst.useValueInBack) {
			nodeInst.addReplacement(" " + operator + " ");
			return nodeInst.merge(inst);
		}

		if (inst.params.size() <= IN_MAX)
			return build(nodeInst, inst);

		Instruction result = new Instruction().addReplacement("("
				+ ClioneUtil.CRLF);
		for (int i = 0; i * IN_MAX >= inst.params.size(); i++) {
			result.addReplacement("\t").addReplacement(i == 0 ? "" : "OR ");
			int start = i * IN_MAX;
			int end = (i + 1) * IN_MAX;
			end = end < inst.params.size() ? end : inst.params.size();
			ArrayList<Object> list = new ArrayList<Object>(end - start);
			System.arraycopy(inst.params, start, list, 0, end - start);

			Instruction subInst = new Instruction();
			subInst.params = list;
			result.merge(build(nodeInst, subInst)).addReplacement(
					ClioneUtil.CRLF);
		}
		result.addReplacement(")");

		return result;
	}

	private Instruction build(Instruction nodeInst, Instruction inst) {
		Instruction result = new Instruction().merge(nodeInst);
		if (!isParamExists(inst)) {
			result.addReplacement(isPositive ? " IS NULL" : " IS NOT NULL");
			return result;
		} else if (inst.params.size() == 1) {
			result.addReplacement(isPositive ? " = " : " <> ");
			return result.merge(inst);
		} else {
			result.addReplacement(isPositive ? " IN " : " NOT IN ");
			if (inst.getReplacement().startsWith("("))
				return result.merge(inst);
			else
				return result.addReplacement("(").merge(inst).addReplacement(
						")");
		}
	}

}
