package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.*;
import org.antlr.v4.codegen.model.extractors.scala.Class;

import java.util.*;

public class INodeToken extends INode implements IFirst {
	private final int index;
	private final boolean constant;
	private final String name;

	public INodeToken(String definingGrammar, int index, boolean constant, String name) {
		super(definingGrammar);
		this.index = index;
		this.constant = constant;
		this.name = name;
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
	public ScalaNode computeType(Namespace namespace) {
		throw new RuntimeException();
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		return getAnon(namespace, alternatives);
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		if(constant) {
			return new Snippet("({it.next; ", namespace.getName(this), "})");
		} else {
			return new Snippet(namespace.getName(this), "(", "it.next.asInstanceOf[TerminalNode].getSymbol.getText", ")");
		}
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		namespace.setNameSuffix(this, this.name);
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		// nop
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		List<ScalaNode> extend = new ArrayList<>();
		for(String rel : alternatives.valuesOf(namespace.getName(this))) {
			extend.add(new Name(rel));
		}

		if(constant) {
			return new Obj(new Name(as), extend);
		} else {
			Decl text = new Decl(new Name("String"), new Name("text"));
			return new Class(new Name(as), Collections.singletonList(text), extend);
		}
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof INodeToken && ((INodeToken) o).index == index;
	}

	public int getIndex() {
		return index;
	}
}
