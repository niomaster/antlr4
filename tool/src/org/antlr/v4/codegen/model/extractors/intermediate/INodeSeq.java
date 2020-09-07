package org.antlr.v4.codegen.model.extractors.intermediate;

import org.antlr.v4.codegen.model.extractors.Namespace;
import org.antlr.v4.codegen.model.extractors.Relation;
import org.antlr.v4.codegen.model.extractors.scala.*;
import org.antlr.v4.codegen.model.extractors.scala.Class;

import java.util.*;

public class INodeSeq extends INode {
	private final List<INode> body;

	public INodeSeq(String definingGrammar, List<INode> body) {
		super(definingGrammar);
		this.body = body;
	}

	public INode flatten() {
		if(body.size() == 1) {
			return body.get(0);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		return "(" + INode.join(this.body, " ") + ")";
	}

	@Override
	public HashSet<IFirst> first() {
		return firstFrom(0);
	}

	private HashSet<IFirst> firstFrom(int index) {
		HashSet<IFirst> result = new HashSet<>(Collections.<IFirst>singleton(INodeEpsilon.INSTANCE));
		for(int i = index; i < body.size(); i++) {
			if(!result.contains(INodeEpsilon.INSTANCE))
				break;

			result.remove(INodeEpsilon.INSTANCE);
			result.addAll(body.get(i).first());
		}

		return result;
	}

	@Override
	public boolean isRepresentable() {
		for(int i = 0; i < body.size(); i++) {
			if(!body.get(i).isRepresentable()) {
				return false;
			}
		}

		for(int i = 0; i < body.size(); i++) {
			Set<IFirst> first = body.get(i).first();

			if(first.contains(INodeEpsilon.INSTANCE)) {
				first.remove(INodeEpsilon.INSTANCE);
				Set<IFirst> follow = firstFrom(i+1);
				follow.remove(INodeEpsilon.INSTANCE);
				first.retainAll(follow);
				if(!first.isEmpty()) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public ScalaNode computeType(Namespace namespace) {
		ArrayList<ScalaNode> ts = new ArrayList<>();
		for(INode node : body) {
			ts.add(node.getType(namespace));
		}
		return new Tuple(ts);
	}

	@Override
	public ScalaNode getNamed(Namespace namespace, Relation<String> alternatives) {
		return new Snippet(namespace.getName(this), getAnon(namespace, alternatives));
	}

	@Override
	public ScalaNode getAnon(Namespace namespace, Relation<String> alternatives) {
		ArrayList<ScalaNode> args = new ArrayList<>();
		boolean first = true;

		for(INode node : body) {
			if(!first) args.add(new Snippet((", ")));
			first = false;

			args.add(node.getNode(namespace, alternatives));
		}

		return new Snippet("(", new Snippet(args.toArray()), ")");
	}

	@Override
	public void collectNames(Namespace namespace, String baseName) {
		for(INode node : body) {
			node.collectNames(namespace, baseName);
		}
	}

	@Override
	public void collectAlts(Namespace namespace, Relation<String> alternatives) {
		for(INode node : body) {
			node.collectAlts(namespace, alternatives);
		}
	}

	@Override
	public ScalaNode getDecl(String as, Namespace namespace, Relation<String> alternatives) {
		ArrayList<Decl> classDecls = new ArrayList<>();

		for (int i = 0; i < body.size(); i++) {
			classDecls.add(new Decl(body.get(i).getType(namespace), new Name("x" + i)));
		}

		List<ScalaNode> extend = new ArrayList<>();
		for (String rel : alternatives.valuesOf(namespace.getName(this))) {
			extend.add(new Name(rel));
		}

		return new Class(new Name(as), classDecls, extend);
	}
}
