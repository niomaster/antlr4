package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import org.antlr.v4.codegen.model.extractors.scala.Seq;
import org.antlr.v4.codegen.model.extractors.scala.Snippet;

import java.util.HashSet;

public class INodeQuantPlus extends INodeQuant {
	public INodeQuantPlus(String definingGrammar, INode body) {
		super(definingGrammar, body);
	}

	@Override
	public String toString() {
		return getBody().toString() + "+";
	}

	@Override
	public HashSet<IFirst> first() {
		return getBody().first();
	}

	@Override
	public boolean isRepresentable() {
		return getBody().isRepresentable();
	}

	@Override
	public ScalaNode computeType(Namespace namespace) {
		return new Seq(getBody().getType(namespace));
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		return new Snippet("{\n",
			"val buf = mutable.ArrayBuffer[", getBody().getType(namespace), "]();\n",
			"while(", consumeMore(namespace, alternatives), "){\n",
				"buf.+=(", getBody().getNode(namespace, alternatives), ")",
			"}\n",
			"buf.toSeq\n",
		"}");
	}
}
