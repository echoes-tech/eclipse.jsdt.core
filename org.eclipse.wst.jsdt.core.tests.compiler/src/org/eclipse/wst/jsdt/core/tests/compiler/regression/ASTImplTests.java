/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.core.tests.compiler.regression;

import org.eclipse.wst.jsdt.core.tests.util.Util;
import org.eclipse.wst.jsdt.internal.compiler.ASTVisitor;
import org.eclipse.wst.jsdt.internal.compiler.CompilationResult;
import org.eclipse.wst.jsdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.wst.jsdt.internal.compiler.ast.Argument;
import org.eclipse.wst.jsdt.internal.compiler.ast.BinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CombinedBinaryExpression;
import org.eclipse.wst.jsdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.ExtendedStringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.JavadocSingleTypeReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.wst.jsdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteral;
import org.eclipse.wst.jsdt.internal.compiler.ast.StringLiteralConcatenation;
import org.eclipse.wst.jsdt.internal.compiler.batch.CompilationUnit;
import org.eclipse.wst.jsdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope;
import org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope;
import org.eclipse.wst.jsdt.internal.compiler.parser.Parser;
import org.eclipse.wst.jsdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.wst.jsdt.internal.compiler.problem.ProblemReporter;

/**
 * A tests series especially meant to validate the internals of our AST
 * implementation.
 */
public class ASTImplTests extends AbstractRegressionTest {
public ASTImplTests(String name) { 
    super(name);
}
  
public static Class testClass() {
    return ASTImplTests.class;
}

// Helper methods
static Parser defaultParser = new Parser(
			new ProblemReporter(DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
			new CompilerOptions(), 
			new DefaultProblemFactory()), false);
public void runConformTest(String fileName, String fileContents, 
		Parser parser, ASTCollector visitor, String expected) {
	CompilationUnit source = 
		new CompilationUnit(fileContents.toCharArray(),	fileName, null);
	CompilationResult compilationResult =
		new CompilationResult(source, 1, 1, 10); 
	CompilationUnitDeclaration unit = parser.parse(source, compilationResult);
	assertEquals(0, compilationResult.problemCount);
	unit.traverse(visitor, unit.scope);
	String result = visitor.result();
	if (! expected.equals(result)) {
		System.out.println(getClass().getName() + '#' + getName());
		System.out.println("Expected:");
		System.out.println(expected);
		System.out.println("But was:");
		System.out.println(result);
		System.out.println("Cut and paste:");
		System.out.println(Util.displayString(result, INDENT, SHIFT));
	}
	assertEquals(expected, result);
}

// AST implementation - visiting binary expressions
public void test0001_regular_binary_expression() {
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    var s3 = \"s3\";\n" + 
		"    var s4 = \"s4\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\" +\n" +
		"      s3 + \"l3\" + s4);\n" + 
		"  }\n" + 
		"\n",
		defaultParser,
		new ASTBinaryExpressionCollector(),
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v SL \"s2\"]\n" + 
		"[ev SL \"s2\"]\n" + 
		"[v SL \"s3\"]\n" + 
		"[ev SL \"s3\"]\n" + 
		"[v SL \"s4\"]\n" + 
		"[ev SL \"s4\"]\n" + 
		"[v BE ((((((s1 + \"l1\") + s...) + s4)]\n" + 
		"[v BE (((((s1 + \"l1\") + s2...+ \"l3\")]\n" + 
		"[v BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v BE ((s1 + \"l1\") + s2)]\n" + 
		"[v BE (s1 + \"l1\")]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[v SL \"l1\"]\n" + 
		"[ev SL \"l1\"]\n" + 
		"[ev BE (s1 + \"l1\")]\n" + 
		"[v SNR s2]\n" + 
		"[ev SNR s2]\n" + 
		"[ev BE ((s1 + \"l1\") + s2)]\n" + 
		"[v SL \"l2\"]\n" + 
		"[ev SL \"l2\"]\n" + 
		"[ev BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v SNR s3]\n" + 
		"[ev SNR s3]\n" + 
		"[ev BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v SL \"l3\"]\n" + 
		"[ev SL \"l3\"]\n" + 
		"[ev BE (((((s1 + \"l1\") + s2...+ \"l3\")]\n" + 
		"[v SNR s4]\n" + 
		"[ev SNR s4]\n" + 
		"[ev BE ((((((s1 + \"l1\") + s...) + s4)]\n");
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions
public void test0002_combined_binary_expression() {
	CombinedBinaryExpression.defaultArityMaxStartingValue = 3;
	// one CBE each fourth BE
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    var s3 = \"s3\";\n" + 
		"    var s4 = \"s4\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\" +\n" +
		"      s3 + \"l3\" + s4);\n" + 
		"  }\n" + 
		"\n",
		defaultParser,
		new ASTBinaryExpressionCollector() {
			public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
				if (binaryExpression instanceof CombinedBinaryExpression &&
						((CombinedBinaryExpression) binaryExpression).
							referencesTable != null) {
					this.collector.append("[ev CBE " + 
						cut(binaryExpression.toString()) + "]\n");
				} else {
					super.endVisit(binaryExpression, scope);
				}
			}
		},
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v SL \"s2\"]\n" + 
		"[ev SL \"s2\"]\n" + 
		"[v SL \"s3\"]\n" + 
		"[ev SL \"s3\"]\n" + 
		"[v SL \"s4\"]\n" + 
		"[ev SL \"s4\"]\n" + 
		"[v BE ((((((s1 + \"l1\") + s...) + s4)]\n" + 
		"[v BE (((((s1 + \"l1\") + s2...+ \"l3\")]\n" + 
		"[v BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v BE ((s1 + \"l1\") + s2)]\n" + 
		"[v BE (s1 + \"l1\")]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[v SL \"l1\"]\n" + 
		"[ev SL \"l1\"]\n" + 
		"[ev BE (s1 + \"l1\")]\n" + 
		"[v SNR s2]\n" + 
		"[ev SNR s2]\n" + 
		"[ev BE ((s1 + \"l1\") + s2)]\n" + 
		"[v SL \"l2\"]\n" + 
		"[ev SL \"l2\"]\n" + 
		"[ev BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v SNR s3]\n" + 
		"[ev SNR s3]\n" + 
		"[ev CBE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v SL \"l3\"]\n" + 
		"[ev SL \"l3\"]\n" + 
		"[ev BE (((((s1 + \"l1\") + s2...+ \"l3\")]\n" + 
		"[v SNR s4]\n" + 
		"[ev SNR s4]\n" + 
		"[ev BE ((((((s1 + \"l1\") + s...) + s4)]\n");
	CombinedBinaryExpression.defaultArityMaxStartingValue = 
		CombinedBinaryExpression.ARITY_MAX_MIN;
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions
public void test0003_combined_binary_expression() {
	Parser parser = new Parser(
			new ProblemReporter(DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
			new CompilerOptions(), 
			new DefaultProblemFactory()), true); // optimize string literals
	CombinedBinaryExpression.defaultArityMaxStartingValue = 2;
		// one CBE each third BE - except the top one, which is degenerate (no
		// references table)
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    var s3 = \"s3\";\n" + 
		"    var s4 = \"s4\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\" +\n" +
		"      s3 + \"l3\" + s4);\n" + 
		"  }\n" + 
		"\n",
		parser,		
		new ASTBinaryExpressionCollector() {
			public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
				if (binaryExpression instanceof CombinedBinaryExpression &&
						((CombinedBinaryExpression) binaryExpression).
							referencesTable != null) {
					this.collector.append("[ev CBE " + 
						cut(binaryExpression.toString()) + "]\n");
				} else {
					super.endVisit(binaryExpression, scope);
				}
			}
		},
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v SL \"s2\"]\n" + 
		"[ev SL \"s2\"]\n" + 
		"[v SL \"s3\"]\n" + 
		"[ev SL \"s3\"]\n" + 
		"[v SL \"s4\"]\n" + 
		"[ev SL \"s4\"]\n" + 
		"[v BE ((((((s1 + \"l1\") + s...) + s4)]\n" + 
		"[v BE (((((s1 + \"l1\") + s2...+ \"l3\")]\n" + 
		"[v BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v BE ((s1 + \"l1\") + s2)]\n" + 
		"[v BE (s1 + \"l1\")]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[v SL \"l1\"]\n" + 
		"[ev SL \"l1\"]\n" + 
		"[ev BE (s1 + \"l1\")]\n" + 
		"[v SNR s2]\n" + 
		"[ev SNR s2]\n" + 
		"[ev BE ((s1 + \"l1\") + s2)]\n" + 
		"[v SL \"l2\"]\n" + 
		"[ev SL \"l2\"]\n" + 
		"[ev CBE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v SNR s3]\n" + 
		"[ev SNR s3]\n" + 
		"[ev BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v SL \"l3\"]\n" + 
		"[ev SL \"l3\"]\n" + 
		"[ev BE (((((s1 + \"l1\") + s2...+ \"l3\")]\n" + 
		"[v SNR s4]\n" + 
		"[ev SNR s4]\n" + 
		"[ev BE ((((((s1 + \"l1\") + s...) + s4)]\n");
	CombinedBinaryExpression.defaultArityMaxStartingValue = 
		CombinedBinaryExpression.ARITY_MAX_MIN;
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - effect of a literal at the start with
// string literal optimization
public void test0004_combined_binary_expression() {
	Parser parser = new Parser(
			new ProblemReporter(DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
			new CompilerOptions(), 
			new DefaultProblemFactory()), true); // optimize string literals
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    println(\"l\" + \"1\" + s1);\n" +
			// "l" + "1" is collapsed into "l1" without affecting binary 
			// expressions: only one BE
		"  }\n" + 
		"\n",
		parser,
		new ASTBinaryExpressionCollector(),
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v BE (ExtendedStringLiter...} + s1)]\n" + 
		"[v ESL ExtendedStringLiteral{l1}]\n" + 
		"[ev ESL ExtendedStringLiteral{l1}]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[ev BE (ExtendedStringLiter...} + s1)]\n");
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - effect of a literal at the start without
// string literals optimization
public void test0005_combined_binary_expression() {
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    println(\"l\" + \"1\" + s1);\n" +
			// "l" + "1" is handled by a string literal concatenation without 
			// affecting binary expressions: only one BE
		"  }\n" + 
		"\n",
		defaultParser,
		new ASTBinaryExpressionCollector(),
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v BE (StringLiteralConcat...} + s1)]\n" + 
		"[v SLC StringLiteralConcate...\n" + 
		"\"1\"+\n" + 
		"}]\n" + 
		"[v SL \"l\"]\n" + 
		"[ev SL \"l\"]\n" + 
		"[v SL \"1\"]\n" + 
		"[ev SL \"1\"]\n" + 
		"[ev SLC StringLiteralConcate...\n" + 
		"\"1\"+\n" + 
		"}]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[ev BE (StringLiteralConcat...} + s1)]\n");
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - cutting the traversal half-way down
public void test0006_combined_binary_expression() {
	CombinedBinaryExpression.defaultArityMaxStartingValue = 1;
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    var s3 = \"s3\";\n" + 
		"    var s4 = \"s4\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\" +\n" +
		"      s3 + s1 + s4);\n" + 
		"  }\n" + 
		"\n",
		defaultParser,
		new ASTBinaryExpressionCollector() {
			public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
				super.visit(binaryExpression, scope);
				if (binaryExpression.right instanceof StringLiteral) {
					return false;
				}
				return true;
			}
		},
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v SL \"s2\"]\n" + 
		"[ev SL \"s2\"]\n" + 
		"[v SL \"s3\"]\n" + 
		"[ev SL \"s3\"]\n" + 
		"[v SL \"s4\"]\n" + 
		"[ev SL \"s4\"]\n" + 
		"[v BE ((((((s1 + \"l1\") + s...) + s4)]\n" + 
		"[v BE (((((s1 + \"l1\") + s2...) + s1)]\n" + 
		"[v BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[ev BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v SNR s3]\n" + 
		"[ev SNR s3]\n" + 
		"[ev BE ((((s1 + \"l1\") + s2)...) + s3)]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[ev BE (((((s1 + \"l1\") + s2...) + s1)]\n" + 
		"[v SNR s4]\n" + 
		"[ev SNR s4]\n" + 
		"[ev BE ((((((s1 + \"l1\") + s...) + s4)]\n");
	CombinedBinaryExpression.defaultArityMaxStartingValue = 
		CombinedBinaryExpression.ARITY_MAX_MIN;
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - cutting the traversal right away
public void test0007_combined_binary_expression() {
	CombinedBinaryExpression.defaultArityMaxStartingValue = 4; 
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    var s3 = \"s3\";\n" + 
		"    var s4 = \"s4\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\" +\n" +
		"      s3 + \"l3\" + s4);\n" + 
		"  }\n" + 
		"\n",
		defaultParser,
		new ASTBinaryExpressionCollector() {
			public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
				super.visit(binaryExpression, scope);
				return false;
			}
		},
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v SL \"s2\"]\n" + 
		"[ev SL \"s2\"]\n" + 
		"[v SL \"s3\"]\n" + 
		"[ev SL \"s3\"]\n" + 
		"[v SL \"s4\"]\n" + 
		"[ev SL \"s4\"]\n" + 
		"[v BE ((((((s1 + \"l1\") + s...) + s4)]\n" + 
		"[ev BE ((((((s1 + \"l1\") + s...) + s4)]\n");
	CombinedBinaryExpression.defaultArityMaxStartingValue = 
		CombinedBinaryExpression.ARITY_MAX_MIN;
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - case of one-deep expression
public void test0008_combined_binary_expression() {
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\");\n" + 
		"    println(s1 + s2);\n" + 
		"  }\n" + 
		"\n",
		defaultParser,		
		new ASTBinaryExpressionCollector() {
			public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
				if (binaryExpression instanceof CombinedBinaryExpression) {
					this.collector.append("[ev CBE " + 
						cut(binaryExpression.toString()) + "]\n");
				} else {
					super.endVisit(binaryExpression, scope);
				}
			}
		},
		"[v SL \"s1\"]\n" + 
		"[ev SL \"s1\"]\n" + 
		"[v SL \"s2\"]\n" + 
		"[ev SL \"s2\"]\n" + 
		"[v BE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v BE ((s1 + \"l1\") + s2)]\n" + 
		"[v BE (s1 + \"l1\")]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[v SL \"l1\"]\n" + 
		"[ev SL \"l1\"]\n" + 
		"[ev BE (s1 + \"l1\")]\n" + 
		"[v SNR s2]\n" + 
		"[ev SNR s2]\n" + 
		"[ev BE ((s1 + \"l1\") + s2)]\n" + 
		"[v SL \"l2\"]\n" + 
		"[ev SL \"l2\"]\n" + 
		"[ev CBE (((s1 + \"l1\") + s2) + \"l2\")]\n" + 
		"[v BE (s1 + s2)]\n" + 
		"[v SNR s1]\n" + 
		"[ev SNR s1]\n" + 
		"[v SNR s2]\n" + 
		"[ev SNR s2]\n" + 
		"[ev BE (s1 + s2)]\n");
}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// check if the generated code is OK when leveraging CombinedBinaryExpression
public void test0009_combined_binary_expression() {
	assertEquals(20, CombinedBinaryExpression.ARITY_MAX_MIN);
	this.runConformTest(
		new String[] {
			"X.js",
			"  function main(args) {\n" + 
			"    var max = 30; \n" + 
			"    var s = new Array();\n" + 
			"    for (var i = 0; i < max; i++) {\n" + 
			"        s[i] = \"a\";\n" + 
			"    }\n" + 
			"    foo(s);\n" + 
			"}\n" + 
			" function foo (s) {\n" + 
			"    println(\n" + 
			"        s[0] + s[1] + s[2] + s[3] + s[4] + s[5] + s[6] + \n" +
			"        s[7] + s[8] + s[9] + s[10] + s[11] + s[12] + s[13] +\n" +
			"        s[14] + s[15] + s[16] + s[17] + s[18] + s[19] + \n" + 
			"        s[20] + s[21] + s[22] + s[23] + s[24] + s[25] + \n" +
			"        s[26] + s[27] + s[28] + s[29]\n" + 
			"        );\n" + 
			"}\n" + 
			""},
		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
}

// AST implementation - visiting binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - checking recursive print
public void test0012_combined_binary_expression() {
	CombinedBinaryExpression.defaultArityMaxStartingValue = 2; 
	runConformTest(
		"X.js", 
		"  function foo() {\n" + 
		"    var s1 = \"s1\";\n" + 
		"    var s2 = \"s2\";\n" + 
		"    var s3 = \"s3\";\n" + 
		"    var s4 = \"s4\";\n" + 
		"    println(s1 + \"l1\" + s2 + \"l2\" +\n" +
		"      s3 + s1 + s4);\n" + 
		"  }\n" + 
		"\n",
		defaultParser,
		new ASTCollector() {
			public boolean visit(BinaryExpression binaryExpression, 
					BlockScope scope) {
				super.visit(binaryExpression, scope);
				this.collector.append(binaryExpression);
				return true;
			}
		},
		"((((((s1 + \"l1\") + s2) + \"l2\") + s3) + s1) + s4)(((((s1 + \"l1\")" +
		" + s2) + \"l2\") + s3) + s1)((((s1 + \"l1\") + s2) + \"l2\") + s3)" +
		"(((s1 + \"l1\") + s2) + \"l2\")((s1 + \"l1\") + s2)(s1 + \"l1\")");
	CombinedBinaryExpression.defaultArityMaxStartingValue = 
		CombinedBinaryExpression.ARITY_MAX_MIN;
}

//// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
//// check if the generated code is OK when leveraging CombinedBinaryExpression
//// variant involving a left-deep right expression at the topmost level
//public void test0013_combined_binary_expression() {
//	assertEquals(20, CombinedBinaryExpression.ARITY_MAX_MIN);
//	this.runConformTest(
//		new String[] {
//			"X.js",
//			"  function main(var args[]) {\n" + 
//			"    final int max = 30; \n" + 
//			"    var s[] = new String[max];\n" + 
//			"    for (int i = 0; i < max; i++) {\n" + 
//			"        s[i] = \"a\";\n" + 
//			"    }\n" + 
//			"    foo(s);\n" + 
//			"}\n" + 
//			" function foo (var s[]) {\n" + 
//			"    println(\n" + 
//			"        \"b\" + (s[0] + s[1] + s[2] + s[3] + s[4] + s[5] + s[6] + \n" +
//			"        s[7] + s[8] + s[9] + s[10] + s[11] + s[12] + s[13] +\n" +
//			"        s[14] + s[15] + s[16] + s[17] + s[18] + s[19] + \n" + 
//			"        s[20] + s[21] + s[22] + s[23] + s[24] + s[25] + \n" +
//			"        s[26] + s[27] + s[28] + s[29])\n" + 
//			"        );\n" + 
//			"}\n" + 
//			"}"
//		},
//		"baaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
//}

//// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
//// check if the generated code is OK when leveraging CombinedBinaryExpression
//// variant involving a left-deep right expression at the topmost level, with
//// a constant high in tree
//public void test0014_combined_binary_expression() {
//	assertEquals(20, CombinedBinaryExpression.ARITY_MAX_MIN);
//	this.runConformTest(
//		new String[] {
//			"X.js",
//			" class X {\n" + 
//			"  function main(var args[]) {\n" + 
//			"    final int max = 30; \n" + 
//			"    var s[] = new String[max];\n" + 
//			"    for (int i = 0; i < max; i++) {\n" + 
//			"        s[i] = \"a\";\n" + 
//			"    }\n" + 
//			"    foo(s);\n" + 
//			"}\n" + 
//			" function foo (var s[]) {\n" +
//			"    final var c = \"c\";\n" + 
//			"    println(\n" + 
//			"        \"b\" + \n" +
//			"         (c + c + c + c + c + c + c + c + c + c + \n" +
//			"          c + c + c + c + c + c + c + c + c + c + \n" +
//			"          c + c + s[0])\n" + 
//			"        );\n" + 
//			"}\n" + 
//			"}"
//		},
//		"bcccccccccccccccccccccca");
//}

// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// check if the generated code is OK when leveraging CombinedBinaryExpression
// variant involving a left-deep right expression at the topmost level, with
// a constant low in tree
//public void test0015_combined_binary_expression() {
//	assertEquals(20, CombinedBinaryExpression.ARITY_MAX_MIN);
//	this.runConformTest(
//		new String[] {
//			"X.js",
//			" class X {\n" + 
//			"  function main(var args[]) {\n" + 
//			"    final int max = 30; \n" + 
//			"    var s[] = new String[max];\n" + 
//			"    for (int i = 0; i < max; i++) {\n" + 
//			"        s[i] = \"a\";\n" + 
//			"    }\n" + 
//			"    foo(s);\n" + 
//			"}\n" + 
//			" function foo (var s[]) {\n" +
//			"    final var c = \"c\";\n" + 
//			"    println(\n" + 
//			"        \"b\" + \n" +
//			"         (c + c + c + c + c + c + c + c + c + c + \n" +
//			"          c + c + c + c + c + c + c + c + c + c + \n" +
//			"          s[0] + s[1] + s[2])\n" + 
//			"        );\n" + 
//			"}\n" + 
//			"}"
//		},
//		"bccccccccccccccccccccaaa");
//}

// AST implementation - binary expressions
// https://bugs.eclipse.org/bugs/show_bug.cgi?id=102728
// Adding combined binary expressions - alternate operands
public void test0016_combined_binary_expression() {
	CombinedBinaryExpression.defaultArityMaxStartingValue = 2; 
	this.runConformTest(
		"X.js", 
		"function foo( i1,  i2,  i3,  i4) {\n" + 
		"  println(i1 - i2 + 0 + i3 + 0 + i4);\n" + 
		"}\n" + 
		"\n",
		defaultParser,
		new ASTCollector() {
			public boolean visit(BinaryExpression binaryExpression, 
					BlockScope scope) {
				super.visit(binaryExpression, scope);
				this.collector.append(binaryExpression);
				return true;
			}
		},
		"(((((i1 - i2) + 0) + i3) + 0) + i4)((((i1 - i2) + 0) + i3) + 0)" +
			"(((i1 - i2) + 0) + i3)((i1 - i2) + 0)(i1 - i2)");
	CombinedBinaryExpression.defaultArityMaxStartingValue = 
		CombinedBinaryExpression.ARITY_MAX_MIN;
}

//
//
////https://bugs.eclipse.org/bugs/show_bug.cgi?id=157170
//public void test0017() {
//	CompilerOptions options = new CompilerOptions();
//	options.complianceLevel = ClassFileConstants.JDK1_5;
//	options.sourceLevel = ClassFileConstants.JDK1_5;
//	options.targetJDK = ClassFileConstants.JDK1_5;
//	this.runConformTest(
//		"X.js", 
//		"@interface Annot {\n" +
//		"	int value() default 0;\n" +
//		"}\n" +
//		"@Annot\n" +
//		"@Annot(3)\n" +
//		"@Annot(value=4)\n" +
//		" class X {\n" + 
//		"}\n",
//		new Parser(
//				new ProblemReporter(DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
//				options, 
//				new DefaultProblemFactory()), false),
//		new AnnotationCollector(),
//		"marker annotation start visit\n" + 
//		"marker annotation end visit\n" + 
//		"single member annotation start visit\n" + 
//		"3\n" + 
//		"single member annotation end visit\n" + 
//		"normal annotation start visit\n" + 
//		"member value pair start visit\n" + 
//		"value, 4\n" + 
//		"member value pair end visit\n" + 
//		"normal annotation end visit\n");
//}
////https://bugs.eclipse.org/bugs/show_bug.cgi?id=157170
//public void test0018() {
//	CompilerOptions options = new CompilerOptions();
//	options.complianceLevel = ClassFileConstants.JDK1_5;
//	options.sourceLevel = ClassFileConstants.JDK1_5;
//	options.targetJDK = ClassFileConstants.JDK1_5;
//	options.docCommentSupport = true;
//	this.runConformTest(
//		"X.js", 
//		"@interface Annot {\n" +
//		"	int value() default 0;\n" +
//		"}\n" +
//		"/**\n" +
//		" * @see Annot\n" +
//		" */\n" +
//		"@Annot\n" +
//		"@Annot(3)\n" +
//		"@Annot(value=4)\n" +
//		" class X {\n" +
//		"	/**\n" +
//		"	 * @see Annot\n" +
//		"	 */\n" +
//		"	public function foo() {}\n" +
//		"}\n",
//		new Parser(
//				new ProblemReporter(DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
//				options, 
//				new DefaultProblemFactory()), false),
//		new AnnotationCollector(),
//		"java doc single type reference start visit\n" + 
//		"java doc single type reference end visit\n" + 
//		"marker annotation start visit\n" + 
//		"marker annotation end visit\n" + 
//		"single member annotation start visit\n" + 
//		"3\n" + 
//		"single member annotation end visit\n" + 
//		"normal annotation start visit\n" + 
//		"member value pair start visit\n" + 
//		"value, 4\n" + 
//		"member value pair end visit\n" + 
//		"normal annotation end visit\n" + 
//		"java doc single type reference start visit\n" + 
//		"java doc single type reference end visit\n");
//}
////https://bugs.eclipse.org/bugs/show_bug.cgi?id=157170
//public void test0019() {
//	CompilerOptions options = new CompilerOptions();
//	options.complianceLevel = ClassFileConstants.JDK1_5;
//	options.sourceLevel = ClassFileConstants.JDK1_5;
//	options.targetJDK = ClassFileConstants.JDK1_5;
//	options.docCommentSupport = true;
//	this.runConformTest(
//		"X.js", 
//		"@interface Annot {\n" +
//		"	int value() default 0;\n" +
//		"}\n" +
//		"/**\n" +
//		" * @see Annot\n" +
//		" */\n" +
//		"@Annot\n" +
//		"@Annot(3)\n" +
//		"@Annot(value=4)\n" +
//		"public class X {\n" +
//		"	/**\n" +
//		"	 * @see Annot\n" +
//		"	 */\n" +
//		"	public function foo(@Annot int i) {\n" +
//		"		@Annot int j = 0;" +
//		"	}\n" +
//		"}\n",
//		new Parser(
//				new ProblemReporter(DefaultErrorHandlingPolicies.proceedWithAllProblems(), 
//				options, 
//				new DefaultProblemFactory()), false),
//		new AnnotationCollector(),
//		"java doc single type reference start visit\n" + 
//		"java doc single type reference end visit\n" + 
//		"marker annotation start visit\n" + 
//		"marker annotation end visit\n" + 
//		"single member annotation start visit\n" + 
//		"3\n" + 
//		"single member annotation end visit\n" + 
//		"normal annotation start visit\n" + 
//		"member value pair start visit\n" + 
//		"value, 4\n" + 
//		"member value pair end visit\n" + 
//		"normal annotation end visit\n" + 
//		"java doc single type reference start visit\n" + 
//		"java doc single type reference end visit\n" + 
//		"start argument\n" + 
//		"marker annotation start visit\n" + 
//		"marker annotation end visit\n" + 
//		"exit argument\n" + 
//		"start local declaration\n" + 
//		"marker annotation start visit\n" + 
//		"marker annotation end visit\n" + 
//		"exit local declaration\n");
//}
}

// Helper classes: define visitors leveraged by some tests
class ASTCollector extends ASTVisitor {
	StringBuffer collector = new StringBuffer();
public String result() {
	return this.collector.toString();
}
}

class ASTBinaryExpressionCollector extends ASTCollector {
static final int LIMIT = 30;
// help limit the output in length by suppressing the middle
// part of strings which length exceeds LIMIT
String cut(String source) {
	int length;
	if ((length = source.length()) > LIMIT) {
		StringBuffer result = new StringBuffer(length);
		result.append(source.substring(0, LIMIT - 10));
		result.append("...");
		result.append(source.substring(length - 7, length));
		return result.toString();
	} else {
		return source;
	}
}
public void endVisit(BinaryExpression binaryExpression, BlockScope scope) {
	this.collector.append("[ev BE " + cut(binaryExpression.toString()) + "]\n");
	super.endVisit(binaryExpression, scope);
}

public void endVisit(ExtendedStringLiteral literal, BlockScope scope) {
	this.collector.append("[ev ESL " + cut(literal.toString()) + "]\n");
	super.endVisit(literal, scope);
}

public void endVisit(SingleNameReference singleNameReference, 
		BlockScope scope) {
	this.collector.append("[ev SNR " + cut(singleNameReference.toString()) + 
		"]\n");
	super.endVisit(singleNameReference, scope);
}

public void endVisit(StringLiteral stringLiteral, BlockScope scope) {
	this.collector.append("[ev SL " + cut(stringLiteral.toString()) + "]\n");
	super.endVisit(stringLiteral, scope);
}

public void endVisit(StringLiteralConcatenation literal, BlockScope scope) {
	this.collector.append("[ev SLC " + cut(literal.toString()) + "]\n");
	super.endVisit(literal, scope);
}

public boolean visit(BinaryExpression binaryExpression, BlockScope scope) {
	this.collector.append("[v BE " + cut(binaryExpression.toString()) + "]\n");
	return super.visit(binaryExpression, scope);
}

public boolean visit(ExtendedStringLiteral literal, BlockScope scope) {
	this.collector.append("[v ESL " + cut(literal.toString()) + "]\n");
	return super.visit(literal, scope);
}

public boolean visit(SingleNameReference singleNameReference, 
		BlockScope scope) {
	this.collector.append("[v SNR " + cut(singleNameReference.toString()) + 
		"]\n");
	return super.visit(singleNameReference, scope);
}

public boolean visit(StringLiteral stringLiteral, BlockScope scope) {
	this.collector.append("[v SL " + cut(stringLiteral.toString()) + "]\n");
	return super.visit(stringLiteral, scope);
}

public boolean visit(StringLiteralConcatenation literal, BlockScope scope) {
	this.collector.append("[v SLC " + cut(literal.toString()) + "]\n");
	return super.visit(literal, scope);
}
}
class AnnotationCollector extends ASTCollector {
public void endVisit(JavadocSingleTypeReference typeRef, BlockScope scope) {
	this.collector.append("java doc single type reference end visit\n");
}
public void endVisit(JavadocSingleTypeReference typeRef, ClassScope scope) {
	this.collector.append("java doc single type reference end visit\n");
}
public boolean visit(JavadocSingleTypeReference typeRef, BlockScope scope) {
	this.collector.append("java doc single type reference start visit\n");
	return true;
}
public boolean visit(JavadocSingleTypeReference typeRef, ClassScope scope) {
	this.collector.append("java doc single type reference start visit\n");
	return true;
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#endVisit(org.eclipse.wst.jsdt.internal.compiler.ast.Argument, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
 */
public void endVisit(Argument argument, BlockScope scope) {
	this.collector.append("exit argument\n");
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#endVisit(org.eclipse.wst.jsdt.internal.compiler.ast.Argument, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
 */
public void endVisit(Argument argument, ClassScope scope) {
	this.collector.append("exit argument\n");
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#endVisit(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
 */
public void endVisit(LocalDeclaration localDeclaration, BlockScope scope) {
	this.collector.append("exit local declaration\n");
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Argument, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
 */
public boolean visit(Argument argument, BlockScope scope) {
	this.collector.append("start argument\n");
	return true;
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.Argument, org.eclipse.wst.jsdt.internal.compiler.lookup.ClassScope)
 */
public boolean visit(Argument argument, ClassScope scope) {
	this.collector.append("start argument\n");
	return true;
}
/* (non-Javadoc)
 * @see org.eclipse.wst.jsdt.internal.compiler.ASTVisitor#visit(org.eclipse.wst.jsdt.internal.compiler.ast.LocalDeclaration, org.eclipse.wst.jsdt.internal.compiler.lookup.BlockScope)
 */
public boolean visit(LocalDeclaration localDeclaration, BlockScope scope) {
	this.collector.append("start local declaration\n");
	return true;
}
}
