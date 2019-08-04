// The MIT License (MIT)
//
// Copyright (c) 2015 Arian Fornaris
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
package phasereditor.inspect.core.jsdoc;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.tm4e.core.grammar.IToken;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.TMUIPlugin;

import phasereditor.inspect.core.InspectCore;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.SwtRM;

/**
 * Class to render JSDoc comments.
 * 
 * @author arian
 *
 */
@SuppressWarnings({ "static-method" })
public class JsdocRenderer {
	private static JsdocRenderer _instance = new JsdocRenderer();

	public JsdocRenderer() {
		_linkPattern = Pattern.compile("\\{@link (?<href>[^\\}]*)\\}");
	}

	public static JsdocRenderer getInstance() {
		return _instance;
	}

	public static String wrapDocBody(String doc) {
		return wrapDocBody(doc, SwtRM.getColor(SWT.COLOR_INFO_BACKGROUND).getRGB(),
				SwtRM.getColor(SWT.COLOR_BLACK).getRGB());
	}

	public static String wrapDocBody(String doc, RGB bg, RGB fg) {
		String bgcolor = "rgb(" + bg.red + ", " + bg.green + ", " + bg.blue + ")";
		String fgcolor = "rgb(" + fg.red + ", " + fg.green + ", " + fg.blue + ")";

		StringBuilder html = new StringBuilder();

		html.append("<html style='background:" + bgcolor + ";color:" + fgcolor + "'><body>");
		html.append("<style>");
		html.append("a { color: " + fgcolor + ";font-weight: bold;}");
		html.append("code { color: blue; font-family: monospace;}");
		html.append("</style>");

		html.append(doc);
		html.append("</body></html>");
		return html.toString();
	}

	private Pattern _linkPattern;

	public String processHtmlDescription(String html) {
		try (StringWriter writer = new StringWriter()) {
			var builder = new HtmlDocumentBuilder(writer, true);
			builder.setEmitAsDocument(false);
			var parser = new MarkupParser();
			parser.setMarkupLanguage(new MarkdownLanguage());
			parser.setBuilder(builder);
			parser.parse(html);

			var html2 = writer.toString();

			html2 = expandCodeTag(html2);

			html2 = expandLinksInHtml(html2);

			return html2;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// var html2 = expandCodeTag(html);
		//
		// return expandLinksInHtml(html2);
	}

	private static String expandCodeTag(String html) {
		var sb = new StringBuilder();
		int i = 0;
		while (i < html.length()) {
			var k = html.indexOf("<code>", i);

			if (k == -1) {
				break;
			}

			var j = html.indexOf("</code>", k);

			if (j == -1) {
				break;
			}

			sb.append(html.substring(i, k));

			String code = html.substring(k + 6, j);

			sb.append(syntaxColoring(code));

			i = j + 7;
		}

		sb.append(html.substring(i));

		return sb.toString();
	}

	private static String syntaxColoring(String code) {
		var grammar = TMEclipseRegistryPlugin.getGrammarRegistryManager().getGrammarForScope("source.js");
		var theme = TMUIPlugin.getThemeManager().getThemeForScope("source.js");

		var sb = new StringBuilder();

		try {
			ITokenizeLineResult result;

			// synchronized (_grammar) {
			result = grammar.tokenizeLine(code);
			// }

			for (IToken token : result.getTokens()) {
				org.eclipse.swt.graphics.Color color = null;

				for (String scope : token.getScopes()) {
					org.eclipse.jface.text.rules.IToken themeToken = theme.getToken(scope);
					if (themeToken != null) {
						Object data = themeToken.getData();
						if (data != null && data instanceof TextAttribute) {
							TextAttribute attr = (TextAttribute) data;
							color = attr.getForeground();
						}
					}
				}

				if (color == null) {
					sb.append(code.substring(token.getStartIndex(), token.getEndIndex()));
				} else {
					sb.append("<span style='color:rgb("

							+ color.getRed() + "," + color.getGreen() + "," + color.getBlue()

							+ ")'>" + code.substring(token.getStartIndex(), token.getEndIndex()) + "</span>");
				}
			}
		} catch (Exception e) {
			// TODO: there are problems with parallel parsing, we should test it in Photon.
			// e.printStackTrace();
		}

		return "<span style='font-family:monospace'>" + sb.toString() + "</span>";
	}

	private String expandLinksInHtml(String html) {
		Matcher matcher = _linkPattern.matcher(html);

		String result = matcher.replaceAll(mr -> {
			String name = mr.group(mr.groupCount());
			String href = name;

			if (name.contains("#")) {
				String[] split = href.split("#");
				name = split[split.length - 1];
			}

			return "<a href='" + href + "'>" + name + "</a>";
		});

		return result;
	}

	public Image getImage(IPhaserMember member) {
		String key = getImageName(member);

		return EditorSharedImages.getImage(key);
	}

	public static String getImageName(IPhaserMember member) {
		String key = null;

		IMemberContainer container = member.getContainer();

		if (member instanceof PhaserType && ((PhaserType) member).isEnum()) {
			key = IEditorSharedImages.IMG_ENUM;
		} else if (member instanceof PhaserProperty) {
			if (container instanceof PhaserType && ((PhaserType) container).isTypeDef()) {
				key = IEditorSharedImages.IMG_FIELD;
			} else {
				key = IEditorSharedImages.IMG_PROPERTY;
			}
		} else if (member instanceof PhaserEventConstant) {
			key = IEditorSharedImages.IMG_EVENT;
		} else if (member instanceof PhaserConstant) {
			key = IEditorSharedImages.IMG_CONSTANT;
		} else if (member instanceof PhaserMethod) {
			if (member.getContainer() instanceof PhaserType) {
				key = IEditorSharedImages.IMG_METHOD;
			} else {
				key = IEditorSharedImages.IMG_FUNTION;
			}
		} else if (member instanceof PhaserType) {
			key = IEditorSharedImages.IMG_CLASS;
		} else {
			key = IEditorSharedImages.IMG_PACKAGE_OBJ;
		}

		return key;
	}

	public Image getGlobalScopeImage() {
		return EditorSharedImages.getImage(IEditorSharedImages.IMG_PACKAGE_OBJ);
	}

	public String render(Object member) {
		if (member == null) {
			return "";
		}

		if (member instanceof PhaserType) {
			return renderType((PhaserType) member);
		}

		if (member instanceof PhaserMethod) {
			return renderMethod((PhaserMethod) member);
		}

		if (member instanceof PhaserEventConstant) {
			return renderEventConstant((PhaserEventConstant) member);
		}

		if (member instanceof PhaserConstant) {
			return renderConstant((PhaserConstant) member);
		}

		if (member instanceof PhaserVariable) {
			return renderVariable((PhaserVariable) member);
		}

		if (member instanceof PhaserNamespace) {
			return renderNamespace((PhaserNamespace) member);
		}

		return member.toString();
	}

	private String renderNamespace(PhaserNamespace member) {
		StringBuilder sb = new StringBuilder();

		sb.append("<b>" + renderImageBase64(getImage(member)) + " " + member.getName() + "</b>");

		sb.append("<p>" + processHtmlDescription(member.getHelp()) + "</p>");

		renderSince(sb, member);

		return sb.toString();
	}

	private String renderImageBase64(Image image) {
		if (image == null) {
			return "";
		}

		String base64 = PhaserEditorUI.imageToBase64(image);

		return "<img src='data:image/png;base64," + base64 + "' style='vertical-align:text-middle'>";
	}

	private String renderEventConstant(PhaserEventConstant event) {
		var sb = new StringBuilder();

		var container = event.getContainer();

		var qname = container.getName() + "." + event.getName();

		sb.append("<b>" + renderImageBase64(getImage(event)) + " event " + qname + "</b>");

		sb.append("<p>" + processHtmlDescription(event.getHelp()) + "</p>");

		renderSince(sb, event);

		sb.append(htmlArgsDoc(event.getArgs()));

		var callers = InspectCore.getPhaserHelp().getMembersMap().values().stream()
				.filter(m -> m.getFiresEventList().contains(event)).collect(toList());

		if (!callers.isEmpty()) {

			sb.append("<p><b>Emitters:</b></p>");

			for (var caller : callers) {
				sb.append(renderLink(caller.getContainer().getName() + "." + caller.getName()));
				sb.append("<br>");
			}
		}

		return sb.toString();
	}

	private String renderConstant(PhaserConstant cons) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(cons.getTypes());

		IMemberContainer container = cons.getContainer();

		String qname = container.getName() + "." + cons.getName();

		sb.append("<b>" + renderImageBase64(getImage(cons)) + returnSignature + " " + qname + "</b>");

		sb.append("<p>" + processHtmlDescription(cons.getHelp()) + "</p>");

		renderSince(sb, cons);

		return sb.toString();
	}

	private String renderVariable(PhaserVariable var) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(var.getTypes());
		IMemberContainer container = var.getContainer();
		String qname = container.getName() + "." + var.getName();

		sb.append("<b>" + renderImageBase64(getImage(var)) + returnSignature + " " + qname + "</b>");

		sb.append("<p>" + processHtmlDescription(var.getHelp()) + "</p>");

		renderSince(sb, var);

		if (var instanceof PhaserProperty && ((PhaserProperty) var).isReadOnly()) {
			sb.append("<p><b>readonly</b></p>");
		}

		renderFires(sb, var);

		return sb.toString();
	}

	public String renderMethod(PhaserMethod method) {
		StringBuilder sb = new StringBuilder();

		String returnSignature = htmlTypes(method.getReturnTypes());

		String qname = method.getContainer().getName() + "." + method.getName();

		sb.append("<b>" + renderImageBase64(getImage(method)) + returnSignature + " " + qname
				+ htmlArgsList(method.getArgs()) + "</b>");

		sb.append("<p>" + processHtmlDescription(method.getHelp()) + "</p>");

		renderSince(sb, method);

		renderFires(sb, method);

		if (method.getReturnTypes().length > 0) {
			if (!method.getFiresEventList().isEmpty()) {
				sb.append("<br>");
			}
			sb.append("<b>Returns:</b> " + returnSignature);
			sb.append("<dd>" + processHtmlDescription(method.getReturnHelp()) + "</dd>");
		}

		sb.append(htmlArgsDoc(method.getArgs()));

		return sb.toString();
	}

	private void renderFires(StringBuilder sb, IPhaserMember member) {
		var list = member.getFiresEventList();

		if (!list.isEmpty()) {

			sb.append("<br><b>Fires:</b><br>");

			for (var event : list) {
				sb.append("<a href='" + event.getContainer().getName() + "." + event.getName() + "'>" + event.getName()
						+ "</a><br>");
			}
		}
	}

	public String renderType(PhaserType type) {
		StringBuilder sb = new StringBuilder();

		sb.append("<b>" + renderImageBase64(getImage(type)) + "class</b> " + type.getName());
		if (!type.getExtends().isEmpty()) {
			sb.append(" <b>extends</b> " + renderExtends(type));
		}
		sb.append("<br>");

		sb.append("<p>" + processHtmlDescription(type.getHelp()) + "</p>");

		renderSince(sb, type);

		renderFires(sb, type);

		sb.append(htmlArgsDoc(type.getConstructorArgs()));

		return sb.toString();
	}

	private void renderSince(StringBuilder sb, IPhaserMember member) {
		var since = member.getSince();
		if (since != null) {
			sb.append("<p><b>Since</b>: " + since + "</p>");
		}
	}

	private String renderExtends(PhaserType type) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String e : type.getExtends()) {
			sb.append((i == 0 ? "" : " | ") + renderLink(e));
			i++;
		}
		return sb.toString();
	}

	private String htmlArgsDoc(List<PhaserMethodArg> args) {
		StringBuilder sb = new StringBuilder();
		if (!args.isEmpty()) {
			sb.append("<br><b>Parameters:</b><br>");
		}

		for (PhaserVariable var : args) {
			sb.append("<b>" + var.getName() + "</b> ");

			if (var.isOptional()) {
				sb.append("[=" + var.getDefaultValue() + "]");
			}

			sb.append(htmlTypes(var.getTypes()));
			sb.append("<dd>" + processHtmlDescription(var.getHelp()) + "</dd>");
			sb.append("<br>");
		}

		return sb.toString();
	}

	private String htmlArgsList(List<PhaserMethodArg> args) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int i = 0;
		for (PhaserVariable arg : args) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(arg.getName());
			i++;
		}
		sb.append(")");
		return sb.toString();
	}

	private String htmlTypes(String[] types) {
		if (types.length == 0) {
			return "void";
		}

		if (types.length == 1) {
			return "{" + renderLink(types[0]) + "}";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int i = 0; i < types.length; i++) {
			if (i > 0) {
				sb.append("|");
			}
			sb.append(renderLink(types[i]));
		}
		sb.append("}");
		return sb.toString();
	}

	private String renderLink(String name) {
		return "<a href='" + name + "'>" + name + "</a>";
	}

}
