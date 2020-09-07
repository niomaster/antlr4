package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.Name;
import org.antlr.v4.codegen.model.extractors.scala.ScalaNode;
import org.antlr.v4.codegen.model.extractors.scala.Snippet;

import java.util.Collections;
import java.util.HashSet;

public class INodeRule extends INode implements IFirst {
	private final int index;
	private final String name;
	private final IGrammar grammar;

	public INodeRule(String definingGrammar, int index, String name, IGrammar grammar) {
		super(definingGrammar);
		this.index = index;
		this.name = name;
		this.grammar = grammar;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public HashSet<IFirst> first() {
		return new HashSet<>(Collections.<IFirst>singleton(this));
	}

	@Override
	public boolean isRepresentable() {
		return true;
	}

	@Override
	public ScalaNode getType(Namespace namespace) {
		return new Name(namespace.getName(this));
	}

	@Override
	public ScalaNode computeType(Namespace namespace) {
		throw new RuntimeException();
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		return getAnon(namespace, alternatives);
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		return new Snippet("read", namespace.getName(this), "(it.next.asInstanceOf[ParserRuleContext])");
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		// nop
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		// nop
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		return null;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof INodeRule && ((INodeRule) o).name.equals(name);
	}

	public String getName() {
		return name;
	}

	public int getIndex() {
		return index;
	}
}
