package org.antlr.v4.codegen.model.extractors.scala;

import org.antlr.v4.codegen.model.extractors.intermediate.INode;

import java.io.PrintStream;
import java.util.List;

public class Tuple extends ScalaNode {
	private final List<ScalaNode> ts;

	public Tuple(List<ScalaNode> ts) {
		this.ts = ts;
	}

	@Override
	public void writeTo(PrintStream out) {
		if (ts.size() == 0) {
			out.print("()");
		} else if (ts.size() == 1) {
			ts.get(0).writeTo(out);
		} else {
			out.print("(");
			writeJoinedBy(out, "", ", ", ts, "");
			out.print(")");
		}
	}
}
