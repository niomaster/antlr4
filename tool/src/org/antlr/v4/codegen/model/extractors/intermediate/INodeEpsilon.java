package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class INodeEpsilon extends INode implements IFirst {
	public static final INodeEpsilon INSTANCE = new INodeEpsilon();

	public INodeEpsilon() {
		super(null);
	}

	@Override
	public String toString() {
		return "";
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
		return new Name("TreeUnit");
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		return new Name(namespace.getName(this));
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		return new Name(namespace.getName(this));
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		namespace.setName(this, "TreeUnit");
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		// nop
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		List<ScalaNode> extend = new ArrayList<>();
		for (String rel : alternatives.valuesOf(namespace.getName(this))) {
			extend.add(new Name(rel));
		}

		return new Obj(new Name(namespace.getName(this)), extend);
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof INodeEpsilon;
	}
}
