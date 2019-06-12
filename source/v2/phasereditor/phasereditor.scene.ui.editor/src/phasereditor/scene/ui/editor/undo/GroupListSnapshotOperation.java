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
package phasereditor.scene.ui.editor.undo;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.scene.ui.editor.SceneEditor;

/**
 * @author arian
 *
 */
public class GroupListSnapshotOperation extends SceneEditorOperation {

	private JSONObject _beforeData;
	private JSONObject _afterData;

	public static JSONObject takeSnapshot(SceneEditor editor) {
		var data = new JSONObject();

		editor.getSceneModel().getGroupsModel().write(data);

		return data;
	}

	public GroupListSnapshotOperation(JSONObject beforeData, JSONObject afterData, String label) {
		super(label);

		_beforeData = beforeData;
		_afterData = afterData;
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo2(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		loadSnapshot(info, _afterData);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo2(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		loadSnapshot(info, _beforeData);

		return Status.OK_STATUS;
	}

	private static void loadSnapshot(IAdaptable info, JSONObject data) {
		var editor = info.getAdapter(SceneEditor.class);
		var model = editor.getSceneModel().getGroupsModel();
		var project = editor.getEditorInput().getFile().getProject();

		model.read(data, project);

		editor.refreshOutline_basedOnId();

		editor.updatePropertyPagesContentWithSelection();

		editor.setDirty(true);
	}

}
