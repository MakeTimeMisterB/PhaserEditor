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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.VisibleComponent;

/**
 * @author arian
 *
 */
public class VisibleSection extends ScenePropertySection {

	private Button _visibleBtn;

	public VisibleSection(ScenePropertyPage page) {
		super("Visible", page);
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof VisibleComponent;
	}

	@Override
	public Control createContent(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);

		comp.setLayout(new GridLayout(1, false));

		_visibleBtn = new Button(comp, SWT.CHECK);
		_visibleBtn.setText("Visible");
		_visibleBtn.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

		update_UI_from_Model();

		return comp;
	}

	@Override
	@SuppressWarnings("boxing")
	public void update_UI_from_Model() {
		var models = List.of(getModels());

		var value = flatValues_to_Boolean(
				models.stream().map(model -> VisibleComponent.get_visible((ObjectModel) model)));

		if (value == null) {
			_visibleBtn.setGrayed(true);
			_visibleBtn.setSelection(false);
		} else {
			_visibleBtn.setGrayed(false);
			_visibleBtn.setSelection(value);
		}

		listen(_visibleBtn, val -> {

			models.forEach(model -> VisibleComponent.set_visible((ObjectModel) model, val));

			_visibleBtn.setGrayed(false);
			_visibleBtn.setSelection(val);

			getEditor().setDirty(true);
		}, models);
	}

}
