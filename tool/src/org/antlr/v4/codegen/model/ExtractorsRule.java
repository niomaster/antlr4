package org.antlr.v4.codegen.model;

import org.antlr.runtime.tree.Tree;
import org.antlr.v4.codegen.OutputModelFactory;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.tool.LeftRecursiveRule;
import org.antlr.v4.tool.Rule;
import org.antlr.v4.tool.ast.AltAST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtractorsRule extends OutputModelObject {
	@ModelElement public List<ExtractorsAlt> alts = new ArrayList<>();

	public ExtractorsRule(OutputModelFactory factory, Rule rule) {
		Tree ruleAST = rule.ast;

		if (rule instanceof LeftRecursiveRule) {
			ruleAST = ((LeftRecursiveRule) rule).originalAST;
		}

		for(Map.Entry<String, List<Pair<Integer, AltAST>>> x : rule.getAltLabels().entrySet()) {
			String label = x.getKey();
			List<Pair<Integer, AltAST>> alts = x.getValue();

			for(Pair<Integer, AltAST> alt : alts) {
				int index = alt.a;
				AltAST altAST = (AltAST)ruleAST.getChild(1).getChild(index-1);
				ExtractorsAlt stAlt = ExtractorsAlt.createFromAltAST(factory, altAST, label);

				if(stAlt == null) {
					System.err.printf("[warning] No unapply generated for alternative %d of rule %s%n", index, rule.name);
				} else {
					this.alts.add(stAlt);
				}
			}
		}
	}
}
