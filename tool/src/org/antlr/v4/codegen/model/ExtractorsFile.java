package org.antlr.v4.codegen.model;

import org.antlr.v4.codegen.OutputModelFactory;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.Rule;

import java.util.ArrayList;
import java.util.List;

public class ExtractorsFile extends OutputFile {
  	public String genPackage; // from -package cmd-line
	public String name;
  	@ModelElement public List<ExtractorsRule> rules;

	public ExtractorsFile(OutputModelFactory factory, String fileName) {
	  	super(factory, fileName);
		Grammar g = factory.getGrammar();
	  	this.genPackage = g.tool.genPackage;
	  	this.name = factory.getGrammar().getRecognizerName();
	  	this.rules = new ArrayList<>();
	  	for(Rule rule : g.rules.values()) {
	  	  	this.rules.add(new ExtractorsRule(factory, rule));
		}
	}
}
