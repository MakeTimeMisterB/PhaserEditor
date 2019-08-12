// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.assetpack.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;

import phasereditor.ui.IEditorSharedImages;
import phasereditor.ui.PatternFilter2;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;

/**
 * @author arian
 *
 */
public class SelectTextureDialog extends Dialog implements IEditorSharedImages {

	private IProject _project;
	private FilteredTree _filteredTree;
	private TreeViewer _viewer;
	private Object _selected;
	private IBaseLabelProvider _labelProvider = new FlatAssetLabelProvider(AssetLabelProvider.GLOBAL_16);
	private IContentProvider _contentProvider = new TextureListContentProvider();
	private String _title;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public SelectTextureDialog(Shell parentShell, String title) {
		super(parentShell);
		_title = title;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId);
		close();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 5;

		_filteredTree = new FilteredTree(container, SWT.BORDER, new PatternFilter2(), true);
		_filteredTree.getViewer().addDoubleClickListener(new IDoubleClickListener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		_viewer = _filteredTree.getViewer();
		_viewer.setLabelProvider(_labelProvider);
		_viewer.setContentProvider(_contentProvider);
		_viewer.setInput(_project);
		_viewer.addSelectionChangedListener(e -> {
			_selected = ((IStructuredSelection) e.getSelection()).getFirstElement();
		});

		AssetPackUI.installAssetTooltips(_viewer);

		return container;
	}

	public IBaseLabelProvider getLabelProvider() {
		return _labelProvider;
	}

	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		_labelProvider = labelProvider;
	}

	public IContentProvider getContentProvider() {
		return _contentProvider;
	}

	public void setContentProvider(IContentProvider contentProvider) {
		_contentProvider = contentProvider;
	}

	@Override
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(_title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(462, 471);
	}

	public void setProject(IProject project) {
		_project = project;
	}

	public IStructuredSelection getSelection() {
		return new StructuredSelection(new Object[] { _selected });
	}

}
