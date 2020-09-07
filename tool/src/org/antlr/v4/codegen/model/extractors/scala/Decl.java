package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;

public class Decl extends ScalaNode {
	private final ScalaNode t;
	private final ScalaNode name;

	public Decl(ScalaNode t, ScalaNode name) {
		this.t = t;
		this.name = name;
	}

	@Override
	public void writeTo(PrintStream out) {
		name.writeTo(out);
		out.print(": ");
		t.writeTo(out);
	}
}
