# ANTLR v4
This is a fork of ANTLR, available at <https://github.com/antlr/antlr4>. These modifications to ANTLR are available under the same license as ANTLR, or the BSD 3-clause license in `LICENSE.txt`.

This fork adds an option `-scala-extractor-objects`, which adds extractor object for each production of each rule. This means that you can apply pattern matching to context objects. Take for example this grammar:

```antlrv4
expr : expr '+' expr
     | number
     ;
```

Now we can match and destructure on the productions of the rule `expr` as such:

```scala
def func(tree: ExprContext): Unit = tree match {
  case Expr0(left, "+", right) => ???
  case Expr1(num) => ???
}
```

There are two caveats to this addition to ANTLR. It forces all rule productions to have alternative names (keeping pre-existing ones), so our grammar above is rewritten to something equivalent to:

```antlrv4
expr : expr '+' expr # expr1
     | number # expr2
     ;
```

Furthermore, this approach builds on the target for Java by adding the extractors to that target. As we can't alter the generated context classes after the fact, the `unapply` methods are defined in a similarly named class, but not the same class. The file with the extractor objects is called `GrammarNameParserPatterns.scala`. The extractors are named the same as the alternatives, so as with the example above the context `<rule.upper>Context` can be destructured with `<rule.upper>0` and `<rule.upper>1`.

Finally it is important to note that currently we can't destructure arbitrary eBNF patterns in rule productions, but you will be warned of this fact.