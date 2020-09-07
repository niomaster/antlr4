package org.antlr.v4.codegen.model.extractors.scala;

import org.antlr.v4.codegen.model.extractors.intermediate.INode;

import java.io.PrintStream;
import java.util.List;

public class Trait extends ScalaNode {
	private final ScalaNode name;
	private final List<ScalaNode> extend;
	private final boolean sealed;

	public Trait(ScalaNode name, List<ScalaNode> extend) {
		this(name, extend, true);
	}

	public Trait(ScalaNode name, List<ScalaNode> extend, boolean sealed) {
		this.name = name;
		this.extend = extend;
		this.sealed = sealed;
	}

	@Override
	public void writeTo(PrintStream out) {
		if(sealed) {
			out.print("sealed ");
		} else {
			out.print("/*unsealed*/ ");
		}
		out.print("trait ");
		name.writeTo(out);
		writeJoinedBy(out, " extends "," with ", extend, "");
	}
}
