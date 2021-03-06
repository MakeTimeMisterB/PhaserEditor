// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.project.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;

import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.properties.FormPropertySection;

/**
 * @author arian
 *
 */
public class ResourcePropertySection extends FormPropertySection<IResource> {

	public ResourcePropertySection() {
		super("Resource");
	}

	@Override
	public boolean supportThisNumberOfModels(int number) {
		return number == 1;
	}

	@Override
	public boolean canEdit(Object obj) {
		return obj instanceof IResource;
	}

	@SuppressWarnings("unused")
	@Override
	public Control createContent(Composite parent) {
		var comp = new Composite(parent, 0);
		comp.setLayout(new GridLayout(2, false));

		{
			label(comp, "Name", "");

			var text = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addUpdate(() -> {
				text.setText(getModels().get(0).getName());
			});
		}

		{
			label(comp, "Project Path", "");
			var text = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addUpdate(() -> {
				var file = getModels().get(0);
				if (file.exists()) {
					text.setText(file.getProjectRelativePath().toPortableString());
				}
			});
		}
		
		{
			label(comp, "Location", "");
			var text = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addUpdate(() -> {
				var file = getModels().get(0);
				if (file.exists()) {
					var location = file.getLocation();
					text.setText(location == null? "" : location.toPortableString());
				}
			});
		}
		
		{
			var comp2 = new Composite(comp, 0);
			comp2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
			comp2.setLayout(new RowLayout());
			var virtual = new Label(comp2, 0);
			virtual.setText("Virtual");
			
			var link = new Label(comp2, 0);
			link.setText("Link");
			addUpdate(() -> {
				var file = getModels().get(0);
				var visible = false;
				if (file.exists()) {
					virtual.setText(file.isVirtual()? "Virtual" : "");
					link.setText(file.isLinked()? "Linked" : "");
					visible = file.isVirtual() || file.isLinked();
				}
				comp2.setVisible(visible);
				var ld = (GridData) comp2.getLayoutData();
				ld.heightHint = visible? -1 : 0;
			});
		}

		{
			label(comp, "Size", "");
			var text = new Text(comp, SWT.READ_ONLY | SWT.BORDER);
			text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			addUpdate(() -> {
				try {
					var res = getModels().get(0);
					if (res instanceof IFile) {
						var file = (IFile) res;
						if (file.exists()) {
							text.setText(PhaserEditorUI.getFileHumanSize(file.getLocation().toFile().length()));
						}
					} else {
						text.setText("");
					}
				} catch (Exception e) {
					ProjectUI.logError(e);
					text.setText("");
				}
			});
		}


		{
			new Label(comp, 0);
			var btn = new Button(comp, SWT.PUSH);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			btn.setText("All Properties");
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				var action = new PropertyDialogAction(
						() -> PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
						createSelectionProvider());
				action.run();
			}));
		}

		return comp;
	}

	private ISelectionProvider createSelectionProvider() {
		return new ISelectionProvider() {

			@Override
			public void setSelection(ISelection selection) {
				//
			}

			@Override
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {
				//
			}

			@Override
			public ISelection getSelection() {
				return new StructuredSelection(getModels());
			}

			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				//
			}
		};
	}

	private void openInSystemExplorer() {
		PhaserEditorUI.showInSystemExplorer(getModels().get(0).getLocation().toFile());
	}

	@Override
	public void fillToolbar(ToolBarManager manager) {
		super.fillToolbar(manager);
		manager.add(new Action("Open In System Explorer", EditorSharedImages.getImageDescriptor(IMG_FOLDER_GO)) {
			@Override
			public void run() {
				openInSystemExplorer();
			}
		});
		manager.add(ProjectUI.getOpenTerminalAction(() -> getModels().get(0)));
	}

}
