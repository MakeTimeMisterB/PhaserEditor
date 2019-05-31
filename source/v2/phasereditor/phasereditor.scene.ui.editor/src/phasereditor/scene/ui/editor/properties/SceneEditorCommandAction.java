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
package phasereditor.scene.ui.editor.properties;

import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.internal.actions.CommandAction;

import phasereditor.scene.ui.editor.SceneEditor;

/**
 * @author arian
 *
 */
public class SceneEditorCommandAction extends CommandAction {

	private SceneEditor _editor;

	public SceneEditorCommandAction(SceneEditor editor, String commandId) {
		super(editor.getEditorSite(), commandId);
		_editor = editor;
	}

	public SceneEditorCommandAction(ScenePropertySection section, String commandId) {
		this(section.getEditor(), commandId);
	}

	@Override
	public void runWithEvent(Event event) {
		_editor.activateCommandContext();

		super.runWithEvent(event);

		_editor.deactivateCommandContext();

	}

}
