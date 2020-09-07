package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;

public class Seq extends ScalaNode {
	private final ScalaNode t;

	public Seq(ScalaNode t) {
		this.t = t;
	}

	@Override
	public void writeTo(PrintStream out) {
		out.print("Seq[");
		t.writeTo(out);
		out.print("]");
	}
}
