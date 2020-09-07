package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.*;

import java.util.HashSet;

public class INodeQuantOpt extends INodeQuant {
	public INodeQuantOpt(String definingGrammar, INode body) {
		super(definingGrammar, body);
	}

	@Override
	public String toString() {
		return getBody().toString() + "?";
	}

	@Override
	public HashSet<IFirst> first() {
		HashSet<IFirst> result = getBody().first();
		result.add(INodeEpsilon.INSTANCE);
		return result;
	}

	@Override
	public boolean isRepresentable() {
		return getBody().isRepresentable();
	}

	@Override
	public ScalaNode computeType(Namespace namespace) {
		return new Option(getBody().getType(namespace));
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		return new Snippet("if(", consumeMore(namespace, alternatives), "){\n",
				"Some(", getBody().getNode(namespace, alternatives), ")\n",
				"} else {\n", "None\n", "}");
	}
}
