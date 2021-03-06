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

import static phasereditor.ui.IEditorSharedImages.IMG_ARROW_REFRESH;
import static phasereditor.ui.PhaserEditorUI.logError;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.actions.MoveResourceAction;
import org.eclipse.ui.actions.NewWizardMenu;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.actions.CommandAction;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.wizards.newresource.BasicNewFolderResourceWizard;
import org.json.JSONArray;

import phasereditor.project.core.ProjectCore;
import phasereditor.project.ui.internal.actions.CopyAction;
import phasereditor.project.ui.internal.actions.PasteAction;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.FilteredTreeCanvas;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.ui.TreeCanvas;
import phasereditor.ui.TreeCanvasDropAdapter;
import phasereditor.ui.TreeCanvasViewer;

/**
 * @author arian
 *
 */
public class ProjectView extends ViewPart implements Consumer<IProject> {

	public static final String ID = "phasereditor.project.ui.projectView";
	FilteredTreeCanvas _filteredTree;
	private TreeCanvasViewer _viewer;
	private IPartListener _partListener;
	private JSONArray _initialExpandedPaths;
	private Clipboard _clipboard;
	private CopyAction _copyAction;
	private PasteAction _pasteAction;
	private DeleteResourceAction _deleteAction;
	private RenameResourceAction _renameAction;
	private MoveResourceAction _moveAction;
	private Action _cleanProjectAction;

	static class ProjectData {
		public List<String> expandedPaths = new ArrayList<>();
		public int scrollValue;
	}

	class MyTreeCanvas extends TreeCanvas {

		public MyTreeCanvas(Composite parent, int style) {
			super(parent, style);
		}

		public void delete() {
			ProjectView.this.delete();
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		_filteredTree = new FilteredTreeCanvas(parent, SWT.NONE) {
			@Override
			protected TreeCanvas createTree() {
				return new MyTreeCanvas(this, SWT.NONE);
			}
		};

		_viewer = new ProjectTreeViewer(_filteredTree.getTree(), new MyContentProvider());

		_viewer.setInput(new Object());

		_filteredTree.getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				openEditor();
			}
		});

		_viewer.getTree().setEditActions(this::copy, null, this::paste);

		createActions();

		createMenus();

		getViewSite().setSelectionProvider(_viewer);

		ProjectCore.addActiveProjectListener(this);

		accept(ProjectCore.getActiveProject());

		restoreState();

		registerWorkbenchListeners();

		init_DnD();

		// we need this to show the right icons, because some content types are not
		// resolved at the first time.
		// swtRun(4000, this::refresh);
	}

	private void init_DnD() {
		Transfer[] types = { LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance() };

		_viewer.addDropSupport(DND.DROP_MOVE, types, new TreeCanvasDropAdapter(_viewer) {

			private IContainer _container;

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (target instanceof IContainer) {
					_container = (IContainer) target;
				} else {
					if (target instanceof IResource) {
						_container = ((IResource) target).getParent();
					}
				}

				if (_container == null) {
					return false;
				}

				if (FileTransfer.getInstance().isSupportedType(transferType)) {
					return true;
				}

				return false;
			}

			@Override
			public boolean performDrop(Object data) {
				var op = new CopyFilesAndFoldersOperation(getSite().getShell());

				String[] filenames = (String[]) data;

				op.copyFiles(filenames, _container);

				return true;
			}

		});
	}

	private void createActions() {
		_clipboard = new Clipboard(getViewSite().getShell().getDisplay());

		_pasteAction = new PasteAction(getViewSite().getShell(), _clipboard);
		_pasteAction.setActionDefinitionId(ActionFactory.PASTE.getId());

		_copyAction = new CopyAction(getViewSite().getShell(), _clipboard, _pasteAction);
		_copyAction.setActionDefinitionId(ActionFactory.COPY.getId());

		_deleteAction = new DeleteResourceAction(getSite());
		_deleteAction.setActionDefinitionId(ActionFactory.DELETE.getId());

		_renameAction = new RenameResourceAction(getSite());
		_renameAction.setActionDefinitionId(ActionFactory.RENAME.getId());

		_moveAction = new MoveResourceAction(getSite());
		_moveAction.setActionDefinitionId(ActionFactory.MOVE.getId());

		var actions = new BaseSelectionListenerAction[] { _pasteAction, _copyAction, _deleteAction, _renameAction,
				_moveAction };

		_viewer.addSelectionChangedListener(e -> {
			var sel = e.getStructuredSelection();
			for (var a : actions) {
				a.selectionChanged(sel);
			}
		});

		var actionBars = getViewSite().getActionBars();

		for (var a : new BaseSelectionListenerAction[] { _deleteAction, _renameAction }) {
			actionBars.setGlobalActionHandler(a.getActionDefinitionId(), a);
		}

		_cleanProjectAction = new Action("Clean Project", EditorSharedImages.getImageDescriptor(IMG_ARROW_REFRESH)) {
			@Override
			public void run() {
				ProjectCore.cleanActiveProject();
			}
		};

	}

	private void createMenus() {
		_viewer.getTree().addMenuDetectListener(new MenuDetectListener() {

			@Override
			public void menuDetected(MenuDetectEvent e) {
				var manager = new MenuManager();

				{
					var sel = _viewer.getStructuredSelection();
					var file = sel.getFirstElement();
					if (file != null && file instanceof IFile) {
						var openAction = new OpenFileAction(getSite().getPage());
						openAction.selectionChanged(sel);
						manager.add(openAction);

						var openWithManager = new MenuManager("Open With...");
						openWithManager.add(new OpenWithMenu(getSite().getPage(), (IFile) file));
						manager.add(openWithManager);
					}

					manager.add(new Separator());
				}

				{
					var manager2 = new MenuManager("New");
					manager2.add(new NewWizardMenu(getSite().getWorkbenchWindow()));
					manager.add(manager2);
					manager.add(createOpenWizard("New Folder",
							"platform:/plugin/org.eclipse.ui.ide/icons/full/etool16/newfolder_wiz.png",
							new BasicNewFolderResourceWizard()));
				}

				manager.add(new Separator());
				manager.add(new CommandAction(getSite(), IWorkbenchCommandConstants.EDIT_COPY));
				manager.add(new CommandAction(getSite(), IWorkbenchCommandConstants.EDIT_PASTE));
				manager.add(new Separator());
				manager.add(new CommandAction(getSite(), IWorkbenchCommandConstants.EDIT_DELETE));
				manager.add(new Separator());
				manager.add(_moveAction);
				manager.add(new CommandAction(getSite(), IWorkbenchCommandConstants.FILE_RENAME));
				manager.add(new Separator());
				manager.add(_cleanProjectAction);
				manager.add(new Separator());
				manager.add(new CommandAction(getSite(), IWorkbenchCommandConstants.FILE_PROPERTIES));

				var menu = manager.createContextMenu(_viewer.getTree());
				menu.setVisible(true);
			}

			private IAction createOpenWizard(String label, String icon, IWorkbenchWizard wizard) {
				var action = new Action(label) {
					@Override
					public void run() {
						var dlg = new WizardDialog(getSite().getShell(), wizard);
						wizard.init(getSite().getWorkbenchWindow().getWorkbench(), _viewer.getStructuredSelection());
						dlg.open();
					}
				};
				action.setImageDescriptor(EditorSharedImages.getImageDescriptor("org.eclipse.ui.ide", icon));
				return action;
			}
		});

		{
			var manager = getViewSite().getActionBars().getToolBarManager();
			manager.add(_cleanProjectAction);
			manager.update(true);
		}
	}

	private void copy() {
		_copyAction.run();
	}

	private void paste() {
		_pasteAction.run();
	}

	private void delete() {
		_deleteAction.run();
	}

	private void restoreState() {
		if (_initialExpandedPaths != null) {
			var list = new ArrayList<>();
			var root = ResourcesPlugin.getWorkspace().getRoot();
			for (int i = 0; i < _initialExpandedPaths.length(); i++) {

				var pathname = _initialExpandedPaths.getString(i);
				var path = new Path(pathname);
				try {
					var folder = root.getFolder(path);
					if (folder.exists()) {
						list.add(folder);
					}
				} catch (Exception e) {
					//
				}

				try {
					var project = root.getProject(pathname);
					if (project.exists()) {
						list.add(project);
					}
				} catch (Exception e) {
					//
				}
			}

			_viewer.setExpandedElements(list.toArray());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object getAdapter(Class adapter) {

		if (adapter == IPropertySheetPage.class) {
			return new ProjectPropertyPage(true);
		}

		return super.getAdapter(adapter);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		if (memento != null) {
			var dataStr = memento.getString("expandedPaths");
			if (dataStr != null) {
				_initialExpandedPaths = new JSONArray(dataStr);
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		var data = new JSONArray();
		var elems = _viewer.getExpandedElements();
		for (var elem : elems) {
			if (elem instanceof IResource) {
				data.put(((IResource) elem).getFullPath().toPortableString());
			}
		}
		memento.putString("expandedPaths", data.toString());
	}

	private void registerWorkbenchListeners() {
		_partListener = new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
				//
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
				//

			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				//

			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
				//

			}

			@Override
			public void partActivated(IWorkbenchPart part) {
				try {
					if (part instanceof IEditorPart) {
						var input = ((IEditorPart) part).getEditorInput();
						if (input instanceof IFileEditorInput) {
							var file = ((IFileEditorInput) input).getFile();
							var project = file.getProject();
							if (project.equals(ProjectCore.getActiveProject())) {
								reveal(file);
							}
						}
					}
				} catch (NullPointerException e) {
					// may happen
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	protected void reveal(IFile file) {
		_viewer.setSelection(new StructuredSelection(file), true);
	}

	protected void openEditor() {
		var sel = (IStructuredSelection) _viewer.getSelection();
		var obj = sel.getFirstElement();
		if (obj != null) {
			if (PhaserEditorUI.externalEditor_enabled()) {

				var useExternal = true;

				if (obj instanceof IFile) {
					var file = (IFile) obj;
					IContentDescription desc;
					try {
						desc = file.getContentDescription();
						var contentType = desc == null ? null : desc.getContentType();
						if (contentType != null) {
							if (contentType.getId().startsWith("phasereditor")) {
								useExternal = false;
							}
						}
					} catch (CoreException e) {
						logError(e);
					}

				}

				if (useExternal) {
					if (obj instanceof IProject) {
						PhaserEditorUI.externalEditor_openProject((IResource) obj);
					} else {
						PhaserEditorUI.externalEditor_openFile((IResource) obj);
					}
					return;
				}
			}

			if (obj instanceof IFile) {
				try {
					IDE.openEditor(getViewSite().getPage(), (IFile) obj);
				} catch (PartInitException e) {
					ProjectCore.logError(e);
				}
			}

		}
	}

	@Override
	public void dispose() {
		ProjectCore.removeActiveProjectListener(this);
		getViewSite().getPage().removePartListener(_partListener);
		_clipboard.dispose();
		super.dispose();
	}

	@Override
	public void setFocus() {
		_filteredTree.setFocus();
	}

	@Override
	public void accept(IProject project) {
		swtRun(this::refresh);
	}

	class MyContentProvider implements ITreeContentProvider {

		public MyContentProvider() {
			super();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			var project = ProjectCore.getActiveProject();

			if (project == null) {
				return new Object[0];
			}

			return new Object[] { project };
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IContainer) {
				try {
					var members = ((IContainer) parentElement).members();
					return Arrays.stream(members)

							.filter(m -> !m.getName().startsWith("."))

							.sorted((a, b) -> {

								var a1 = a instanceof IContainer ? 0 : 1;
								var b1 = b instanceof IContainer ? 0 : 1;

								return Integer.compare(a1, b1);

							})

							.toArray();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof IResource) {
				return ((IResource) element).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

	}

	public void refresh() {
		_viewer.getTree().setRedraw(false);
		var expanded = _viewer.getExpandedElements();
		_viewer.refresh();
		_viewer.setExpandedElements(expanded);
		_viewer.getTree().setRedraw(true);
		_viewer.getTree().redraw();
	}

}
