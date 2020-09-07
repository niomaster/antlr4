package org.antlr.v4.codegen.model.extractors.scala;

import java.io.PrintStream;
import java.util.List;

public class Class extends ScalaNode {
	private final ScalaNode name;
	private final List<Decl> params;
	private final List<ScalaNode> extend;

	public Class(ScalaNode name, List<Decl> params, List<ScalaNode> extend) {
		this.name = name;
		this.params = params;
		this.extend = extend;
	}

	@Override
	public void writeTo(PrintStream out) {
		out.print("case class ");
		name.writeTo(out);
		out.print("(");
		writeJoinedBy(out, "", ", ", params, "");
		out.print(")");
		writeJoinedBy(out, " extends ", " with ", extend, "");
	}
}
