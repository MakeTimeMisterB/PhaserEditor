// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.core.codegen;

import phasereditor.project.core.codegen.BaseCodeGenerator;
import phasereditor.scene.core.codedom.AssignPropertyDom;
import phasereditor.scene.core.codedom.ClassDeclDom;
import phasereditor.scene.core.codedom.CodeDom;
import phasereditor.scene.core.codedom.MemberDeclDom;
import phasereditor.scene.core.codedom.MethodCallDom;
import phasereditor.scene.core.codedom.MethodDeclDom;
import phasereditor.scene.core.codedom.RawCode;
import phasereditor.scene.core.codedom.UnitDom;

/**
 * @author arian
 *
 */
public class JS6_UnitCodeGenerator extends BaseCodeGenerator {

	private UnitDom _unit;

	public JS6_UnitCodeGenerator(UnitDom unit) {
		_unit = unit;
	}

	@Override
	protected void internalGenerate() {

		sectionStart("/* START OF COMPILED CODE */", "\n// You can write more code here\n\n");

		line();
		line();

		for (var elem : _unit.getElements()) {

			generateUnitElement(elem);

		}

		section("/* END OF COMPILED CODE */", "\n\n// You can write more code here\n");

	}

	private void generateUnitElement(Object elem) {

		if (elem instanceof ClassDeclDom) {

			generateClass((ClassDeclDom) elem);

		} else if (elem instanceof MethodDeclDom) {

			line();

			generateMethodDecl((MethodDeclDom) elem, true);

			line();
		}
	}

	private void generateClass(ClassDeclDom clsDecl) {

		append("class " + clsDecl.getName() + " ");

		if (clsDecl.getSuperClass() != null && clsDecl.getSuperClass().trim().length() > 0) {
			append("extends " + clsDecl.getSuperClass() + " ");
		}

		openIndent("{");

		line("");

		for (var memberDecl : clsDecl.getMembers()) {
			generateMemberDecl(memberDecl);
			line();
		}

		section("/* START-USER-CODE */", "/* END-USER-CODE */", "\n\n\t// Write your code here.\n\n\t");

		closeIndent("}");

		line();
	}

	protected void generateMemberDecl(MemberDeclDom memberDecl) {

		if (memberDecl instanceof MethodDeclDom) {
			generateMethodDecl((MethodDeclDom) memberDecl, false);
		}

	}

	private void generateMethodDecl(MethodDeclDom methodDecl, boolean function) {
		if (function) {
			append("function ");
		}

		append(methodDecl.getName() + "() ");

		line("{");
		openIndent();

		for (var instr : methodDecl.getInstructions()) {
			generateInstr(instr);
		}

		closeIndent("}");
	}

	private void generateInstr(CodeDom instr) {

		instr.setOffset(getOffset());

		if (instr instanceof RawCode) {

			generateRawCode(((RawCode) instr));

		} else if (instr instanceof MethodCallDom) {

			generateMethodCall((MethodCallDom) instr);

		} else if (instr instanceof AssignPropertyDom) {

			generateAssignProperty((AssignPropertyDom) instr);

		}
	}

	private void generateAssignProperty(AssignPropertyDom assign) {
		{
			var type = assign.getPropertyType();
			if (type != null) {
				line("/** @type {" + type + "} */");
			}
		}
		append(assign.getContextExpr());
		append(".");
		append(assign.getPropertyName());
		append(" = ");
		append(assign.getPropertyValueExpr());
		append(";");
		line();
	}

	private void generateMethodCall(MethodCallDom call) {
		if (call.getReturnToVar() != null) {
			if (call.isDeclareReturnToVar()) {
				append("var ");
			}
			append(call.getReturnToVar());
			append(" = ");
		}

		if (call.getContextExpr() != null) {
			append(call.getContextExpr());
			append(".");
		}
		append(call.getMethodName());
		append("(");

		join(call.getArgs());

		line(");");
	}

	private void generateRawCode(RawCode raw) {

		var code = raw.getCode();

		var lines = code.split("\\R");

		for (var line : lines) {
			line(line);
		}
	}
}
