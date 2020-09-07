package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.HashSet;

public class INodeBail extends INode {
	public INodeBail(String definingGrammar) {
		super(definingGrammar);
	}

	@Override
	public String toString() {
		return "<<unknown>>";
	}

	@Override
	public HashSet<IFirst> first() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isRepresentable() {
		return false;
	}

	@Override
	public ScalaNode computeType(Namespace namespace) {
		throw new NotImplementedException();
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		throw new RuntimeException();
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		throw new RuntimeException();
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		throw new NotImplementedException();
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		throw new NotImplementedException();
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		throw new NotImplementedException();
	}
}
