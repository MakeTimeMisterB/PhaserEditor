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
package phasereditor.assetpack.ui.preview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;

import phasereditor.assetpack.core.AtlasAssetModel;
import phasereditor.assetpack.core.AtlasAssetModel.FrameItem;
import phasereditor.assetpack.ui.AssetLabelProvider;
import phasereditor.atlas.ui.AtlasCanvas;

@SuppressWarnings("synthetic-access")
public class AtlasAssetPreviewComp extends Composite {
	static final Object NO_SELECTION = "none";

	private class AtlasLabelProvider extends AssetLabelProvider {

		public AtlasLabelProvider() {
		}

		@SuppressWarnings("boxing")
		@Override
		public String getText(Object element) {
			if (element == NO_SELECTION) {
				return "(None)";
			}

			if (element instanceof FrameItem) {
				FrameItem fd = (FrameItem) element;
				return String.format("%s (%dx%d)", fd.getName(), fd.getSourceW(), fd.getSourceH());
			}
			return super.getText(element);
		}
	}

	private ComboViewer _spritesViewer;
	private AtlasCanvas _atlasCanvas;
	private AtlasAssetModel _model;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public AtlasAssetPreviewComp(Composite parent, int style) {
		super(parent, style);

		setData("org.eclipse.e4.ui.css.CssClassName", "DarkComposite");

		setLayout(new GridLayout(1, false));

		_spritesViewer = new ComboViewer(this, SWT.READ_ONLY);
		_spritesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				spriteSelected();
			}
		});
		Combo _table = _spritesViewer.getCombo();
		_table.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		_spritesViewer.setContentProvider(new ArrayContentProvider());
		_spritesViewer.setLabelProvider(new AtlasLabelProvider());

		_atlasCanvas = new AtlasCanvas(this, SWT.NONE);
		_atlasCanvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				atlasCanvasClicked();
			}
		});
		_atlasCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		afterCreateWidgets();
	}

	private void afterCreateWidgets() {
		// nothing
		DragSource dragSource = new DragSource(_atlasCanvas, DND.DROP_MOVE | DND.DROP_DEFAULT);
		dragSource.setTransfer(new Transfer[] { TextTransfer.getInstance(), LocalSelectionTransfer.getTransfer() });
		dragSource.addDragListener(new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				ISelection sel = getSelection();
				if (sel.isEmpty()) {
					event.doit = false;
					return;
				}
				LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
				transfer.setSelection(sel);
			}

			private ISelection getSelection() {
				if (_atlasCanvas.isSingleFrame()) {
					return StructuredSelection.EMPTY;
				}

				FrameItem over = (FrameItem) _atlasCanvas.getOverFrame();

				if (over == null) {
					return StructuredSelection.EMPTY;
				}

				return new StructuredSelection(over);
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				IStructuredSelection sel = _spritesViewer.getStructuredSelection();
				Object[] elems = sel.toArray();
				if (elems.length == 1) {
					Object elem = elems[0];
					//event.data = AssetPackCore.getAssetStringReference(elem);
					event.data = elem;
				}
			}
		});
	}

	protected void spriteSelected() {
		IStructuredSelection sel = (IStructuredSelection) _spritesViewer.getSelection();
		Object elem = sel.getFirstElement();
		FrameItem frame;
		if (elem == NO_SELECTION) {
			frame = null;
		} else {
			frame = (FrameItem) elem;
		}
		_atlasCanvas.setFrame(frame);
		_atlasCanvas.redraw();
	}

	protected void atlasCanvasClicked() {
		// if single frame mode we do not have nothing to select.

		if (_atlasCanvas.isSingleFrame()) {
			return;
		}

		// select the last pointed frame

		FrameItem over = (FrameItem) _atlasCanvas.getOverFrame();
		_atlasCanvas.setFrame(over);
		_atlasCanvas.redraw();
		Combo combo = _spritesViewer.getCombo();
		if (over == null) {
			combo.select(-1);
		} else {
			_spritesViewer.setSelection(new StructuredSelection(over), true);
			combo.forceFocus();
		}
	}

	public void setModel(AtlasAssetModel model) {
		_model = model;
		String url = model.getTextureURL();
		IFile file = model.getFileFromUrl(url);
		_atlasCanvas.setImageFile(file);
		List<FrameItem> frames = model.getAtlasFrames();
		_atlasCanvas.setFrames(frames);
		_atlasCanvas.redraw();

		{
			List<Object> input = new ArrayList<>(frames);
			input.add(0, NO_SELECTION);
			_spritesViewer.setInput(input);
		}

		if (!frames.isEmpty()) {
			selectElement(NO_SELECTION);
		}
	}

	public AtlasAssetModel getModel() {
		return _model;
	}

	public void selectElement(Object element) {
		_spritesViewer.setSelection(new StructuredSelection(element));
	}

	public AtlasCanvas getAtlasCanvas() {
		return _atlasCanvas;
	}
}
