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
package phasereditor.assetpack.ui.editor;

import static java.util.stream.Collectors.joining;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import phasereditor.assetpack.core.IEditableKey;
import phasereditor.ui.properties.TextListener;

/**
 * @author arian
 *
 */
public class KeySection extends AssetPackEditorSection<IEditableKey> {

	public KeySection(AssetPackEditorPropertyPage page) {
		super(page, "Key");
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IEditableKey;
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number > 0;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		label(comp, "Key", "*The file key");

		var text = new Text(comp, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		new TextListener(text) {

			@Override
			protected void accept(String value) {
				wrapOperation(() -> {
					getModels().forEach(model -> model.setKey(value));
				});
			}
		};

		addUpdate(() -> {
			var size = getModels().size();

			if (size == 1) {
				text.setText(getModels().get(0).getKey());
			} else {
				text.setText("[" + getModels().stream().map(m -> m.getKey()).collect(joining(",")) + "]");
			}
			text.setEditable(size == 1);
		});

		return comp;
	}
}
