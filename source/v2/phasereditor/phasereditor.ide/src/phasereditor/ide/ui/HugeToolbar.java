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
package phasereditor.ide.ui;

import static phasereditor.ui.IEditorSharedImages.IMG_ALIEN_EDITOR;
import static phasereditor.ui.IEditorSharedImages.IMG_ARROW_REFRESH;
import static phasereditor.ui.IEditorSharedImages.IMG_GAME_CONTROLLER;
import static phasereditor.ui.IEditorSharedImages.IMG_SEARCH;
import static phasereditor.ui.PhaserEditorUI.getWorkbenchWindow;
import static phasereditor.ui.PhaserEditorUI.swtRun;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.NewWizardDropDownAction;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.quickaccess.QuickAccessDialog;
import org.eclipse.ui.statushandlers.StatusManager;

import phasereditor.ide.IDEPlugin;
import phasereditor.ide.ui.wizards.NewWizardLauncherDialog;
import phasereditor.project.core.ProjectCore;
import phasereditor.project.ui.ProjectUI;
import phasereditor.ui.EditorSharedImages;
import phasereditor.ui.IEditorHugeToolbar;
import phasereditor.ui.ISuperWorkbenchListener;
import phasereditor.ui.PhaserEditorUI;
import phasereditor.webrun.ui.WebRunUI;

class HugeToolbar extends Composite implements ISuperWorkbenchListener {

	private Composite _centerArea;
	private IEditorPart _activeEditor;
	private IWorkbenchWindow _window;

	@SuppressWarnings("unused")
	public HugeToolbar(Composite parent) {
		super(parent, 0);

		var gridLayout = new GridLayout(3, false);
		gridLayout.marginWidth = gridLayout.marginHeight = 0;
		setLayout(gridLayout);

		parent.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				updateBounds();
			}

			@Override
			public void controlMoved(ControlEvent e) {
				updateBounds();
			}
		});

		var layout = new RowLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.center = true;

		var leftArea = new Composite(this, 0);
		leftArea.setLayout(layout);
		leftArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		{
			_centerArea = new Composite(this, 0);
			_centerArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			_centerArea.setLayout(layout);
		}

		var rightArea = new Composite(this, 0);
		rightArea.setLayout(layout);

		new GoHomeWrapper(leftArea);
		new NewMenuWrapper(leftArea);
		new RunProjectWrapper(leftArea);
		new AlienEditorWrapper(leftArea);
		new QuickAccessWrapper(rightArea);

		{
			var sep = new Label(rightArea, SWT.SEPARATOR);
			sep.setLayoutData(new RowData(SWT.DEFAULT, 15));
		}

		new PerspectiveSwitcherWrapper(rightArea);

		updateBounds();

		// run async because the window is not finished
		swtRun(() -> {
			_window = getWorkbenchWindow(parent);

			if (_window == null) {
				return;
			}

			_window.addPageListener(this);
			pageOpened(_window.getActivePage());
		});

	}

	private void updateBounds() {
		var size = computeSize(SWT.DEFAULT, SWT.DEFAULT);
		setBounds(0, 0, getParent().getClientArea().width, size.y);
	}

	private void updateWithCurrentEditor() {
		var editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor == _activeEditor) {
			return;
		}

		for (var c : _centerArea.getChildren()) {
			c.dispose();
		}

		var editorToolbar = editor.getAdapter(IEditorHugeToolbar.class);
		if (editorToolbar != null) {
			editorToolbar.createContent(_centerArea);
		}

		_centerArea.requestLayout();
	}

	@Override
	public void pageOpened(IWorkbenchPage page) {
		page.addPartListener(this);
		updateWithCurrentEditor();
	}

	@Override
	public void pageClosed(IWorkbenchPage page) {
		page.removePartListener(this);
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		updateWithCurrentEditor();
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		updateWithCurrentEditor();
	}
}

class NewMenuWrapper {

	private Button _btn;

	public NewMenuWrapper(Composite parent) {
		_btn = new Button(parent, SWT.PUSH);
		_btn.setText("New");
		_btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var dlg = new NewWizardLauncherDialog(_btn.getShell());
			dlg.open();
		}));
		var action = new NewWizardDropDownAction(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		_btn.setImage(action.getImageDescriptor().createImage());
	}

	public Button getButton() {
		return _btn;
	}

}

class GoHomeWrapper {
	public GoHomeWrapper(Composite parent) {
		var btn = new Button(parent, SWT.PUSH);
		var homePersp = PlatformUI.getWorkbench().getPerspectiveRegistry().findPerspectiveWithId(StartPerspective.ID);
		btn.setImage(PerspectiveSwitcherWrapper.getPerspectiveIcon(homePersp));
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(
				e -> PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setPerspective(homePersp)));
		btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button == 3) {
					var serv = PlatformUI.getWorkbench().getService(ICommandService.class);
					try {
						serv.getCommand(ProjectUI.CMD_OPEN_PROJECT).executeWithChecks(new ExecutionEvent());
					} catch (Exception e1) {
						IDEPlugin.logError(e1);
					}
				}
			}
		});
	}
}

class RunProjectWrapper {
	public RunProjectWrapper(Composite parent) {
		var btn = new Button(parent, SWT.PUSH);
		btn.setText("Play");
		btn.setImage(EditorSharedImages.getImage(IMG_GAME_CONTROLLER));
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> WebRunUI
				.openBrowser(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getSelection())));
	}
}

class AlienEditorWrapper {
	private Button _btn;

	public AlienEditorWrapper(Composite parent) {
		_btn = new Button(parent, SWT.PUSH);
		_btn.setText("Editor");
		_btn.setToolTipText("Open this project in the configured External Editor.");
		_btn.setImage(EditorSharedImages.getImage(IMG_ALIEN_EDITOR));
		_btn.addSelectionListener(SelectionListener
				.widgetSelectedAdapter(e -> PhaserEditorUI.externalEditor_openProject(ProjectCore.getActiveProject())));

		{
			IPropertyChangeListener listener = e -> {
				if (PhaserEditorUI.PREF_PROP_ALIEN_EDITOR_ENABLED.equals(e.getProperty())) {
					updateButton();
				}
			};
			PhaserEditorUI.getPreferenceStore().addPropertyChangeListener(listener);
			_btn.addDisposeListener(e -> PhaserEditorUI.getPreferenceStore().removePropertyChangeListener(listener));
		}

		updateButton();
	}

	private void updateButton() {
		var visible = PhaserEditorUI.externalEditor_enabled();
		_btn.setLayoutData(new RowData(visible ? SWT.DEFAULT : 0, visible ? SWT.DEFAULT : 0));
		_btn.getParent().requestLayout();
	}

	public Button getButton() {
		return _btn;
	}

}

class PerspectiveSwitcherWrapper implements IPerspectiveListener {

	private Button _btn;
	private IPerspectiveDescriptor _currentPersp;

	public PerspectiveSwitcherWrapper(Composite parent) {

		{
			var btn = new Button(parent, SWT.PUSH);
			PlatformUI.getWorkbench().getSharedImages().getImage(IWorkbenchGraphicConstants.IMG_ETOOL_NEW_FASTVIEW);
			btn.setImage(EditorSharedImages.getImageDescriptor("org.eclipse.ui", "icons/full/etool16/new_fastview.png")
					.createImage());
			btn.setToolTipText("Add a new view to this perspective.");
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
				var service = PlatformUI.getWorkbench().getService(IHandlerService.class);
				try {
					service.executeCommand("org.eclipse.ui.views.showView", null);
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e1) {
					e1.printStackTrace();
				}
			}));
		}

		{
			var btn = new Button(parent, SWT.PUSH);
			btn.setImage(EditorSharedImages.getImage(IMG_ARROW_REFRESH));
			btn.setToolTipText("Reset persepctive.");
			btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(
					e -> PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().resetPerspective()));
		}

		{
			_btn = new Button(parent, SWT.PUSH);
			_btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> populateMenu()));
			_btn.addMouseListener(MouseListener.mouseUpAdapter(this::populatePropsMenu));
			_btn.setLayoutData(new RowData(150, SWT.DEFAULT));
			updateButton();
		}

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(this);
	}

	private void populatePropsMenu(MouseEvent e) {
		if (e.button == 3) {
			var manager = new MenuManager();
			manager.add(new Action("Reset") {
				@Override
				public void run() {
					getActivePage().resetPerspective();
				}
			});
			showMenu(manager);
		}
	}

	/**
	 * The action for that allows the user to choose any perspective to open.
	 *
	 * @since 3.1
	 */
	private Action _openOtherAction = new Action(WorkbenchMessages.PerspectiveMenu_otherItem) {
		@Override
		public final void runWithEvent(final Event event) {
			runOther(new SelectionEvent(event));
		}
	};

	/**
	 * Show the "other" dialog, select a perspective, and run it. Pass on the
	 * selection event should the menu need it.
	 *
	 * @param event
	 *            the selection event
	 */
	static void runOther(SelectionEvent event) {
		IHandlerService handlerService = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getService(IHandlerService.class);
		try {
			handlerService.executeCommand(IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE, null);
		} catch (Exception e) {
			StatusManager.getManager().handle(new Status(IStatus.WARNING, WorkbenchPlugin.PI_WORKBENCH,
					"Failed to execute " + IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE, e)); //$NON-NLS-1$
		}
	}

	private static String[] EDITOR_PERSPECTIVES = { /* StartPerspective.ID, */ CodePerspective.ID, ScenePerspective.ID,
			LabsPerspectiveFactory.ID, "org.eclipse.egit.ui.GitRepositoryExploring" };

	private void populateMenu() {
		var manager = new MenuManager();

		var reg = PlatformUI.getWorkbench().getPerspectiveRegistry();
		var current = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId();

		for (var id : EDITOR_PERSPECTIVES) {

			if (id.equals(current)) {
				continue;
			}

			var persp = reg.findPerspectiveWithId(id);

			if (persp == null) {
				continue;
			}

			manager.add(new Action(persp.getLabel(), persp.getImageDescriptor()) {
				{
					setDescription(persp.getDescription());
				}

				@Override
				public void run() {
					getActivePage().setPerspective(persp);
				}
			});
		}

		manager.add(_openOtherAction);

		showMenu(manager);
	}

	private void showMenu(MenuManager manager) {
		var menu = manager.createContextMenu(_btn);
		menu.setVisible(true);
	}

	private void updateButton() {
		var persp = getActivePage().getPerspective();

		if (_currentPersp != persp) {
			_currentPersp = persp;
			_btn.setText(persp.getLabel());

			var img = getPerspectiveIcon(persp);

			_btn.setImage(img);
			_btn.setToolTipText("Perspective: " + persp.getDescription());

		}
	}

	public static Image getPerspectiveIcon(IPerspectiveDescriptor persp) {
		var imgReg = IDEPlugin.getDefault().getImageRegistry();
		var img = imgReg.get(persp.getId());
		if (img == null) {
			img = persp.getImageDescriptor().createImage();
			imgReg.put(persp.getId(), img);
		}
		return img;
	}

	public Button getButton() {
		return _btn;
	}

	@Override
	public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		updateButton();
	}

	@Override
	public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
		//
	}

	private static IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
}

class QuickAccessWrapper {
	private Button _button;

	public QuickAccessWrapper(Composite parent) {
		var btn = new Button(parent, SWT.PUSH);
		btn.setImage(EditorSharedImages.getImage(IMG_SEARCH));
		btn.setText("Quick Access");
		btn.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			var dlg = new QuickAccessDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), null);
			dlg.open();
		}));
		_button = btn;
	}

	public Button getButton() {
		return _button;
	}
}