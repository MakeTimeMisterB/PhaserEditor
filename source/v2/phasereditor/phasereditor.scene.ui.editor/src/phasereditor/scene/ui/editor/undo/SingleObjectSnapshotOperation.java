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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.json.JSONObject;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.ui.editor.SceneEditor;
import phasereditor.scene.ui.editor.messages.UpdateObjectsMessage;

/**
 * @author arian
 */
public class SingleObjectSnapshotOperation extends SceneEditorOperation {

	public static List<JSONObject> takeSnapshot(List<ObjectModel> models) {
		var list = new ArrayList<JSONObject>();

		for (var model : models) {
			var data = new JSONObject();
			model.write(data);
			list.add(data);
		}

		return list;
	}

	private List<JSONObject> _beforeData;
	private List<JSONObject> _afterData;

	public SingleObjectSnapshotOperation(List<JSONObject> beforeData, List<JSONObject> afterData, String label,
			boolean needsFullReload) {
		super(label, needsFullReload);
		_beforeData = beforeData;
		_afterData = afterData;
	}

	public SingleObjectSnapshotOperation(List<JSONObject> beforeData, List<JSONObject> afterData, String label) {
		this(beforeData, afterData, label, false);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		// nothing to do, the change was made
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

	private static void loadSnapshot(IAdaptable info, List<JSONObject> snapshot) {
		var editor = info.getAdapter(SceneEditor.class);
		var sceneModel = editor.getSceneModel();
		var project = editor.getEditorInput().getFile().getProject();
		var selectionIds = editor.getSelectionIdList();

		for (var data : snapshot) {
			var id = data.getString("-id");

			var model = sceneModel.getDisplayList().findById(id);

			if (model == null) {
				model = sceneModel.getGroupsModel().findById(id);
			}

			if (model != null) {
				model.read(data, project);
			}
		}

		editor.refreshOutline_basedOnId();

		editor.setSelectionFromIdList(selectionIds);

		editor.updatePropertyPagesContentWithSelection();

		editor.setDirty(true);

		editor.getBroker().sendAll(UpdateObjectsMessage.createFromSnapshot(snapshot));
	}

}
