var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide_1) {
        var ide = colibri.ui.ide;
        var controls = colibri.ui.controls;
        ide_1.ICON_PLAY = "play";
        class IDEPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.ide");
                this._openingProject = false;
            }
            static getInstance() {
                return this._instance;
            }
            registerExtensions(reg) {
                // windows
                reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID, new colibri.ui.ide.WindowExtension("phasereditor2d.ide.ui.DesignWindow", () => new ide_1.ui.DesignWindow()));
                reg.addExtension(colibri.ui.ide.WindowExtension.POINT_ID, new colibri.ui.ide.WindowExtension("phasereditor2d.ide.ui.WelcomeWindow", () => new ide_1.ui.WelcomeWindow()));
                // icons
                reg.addExtension(colibri.ui.ide.IconLoaderExtension.POINT_ID, new colibri.ui.ide.IconLoaderExtension("phasereditor2d.ide.ui.IconLoader", [
                    this.getIcon(ide_1.ICON_PLAY)
                ]));
                // keys
                reg.addExtension(colibri.ui.ide.commands.CommandExtension.POINT_ID, new colibri.ui.ide.commands.CommandExtension("phasereditor2d.welcome.ui.actions.WelcomeActions", ide_1.ui.actions.IDEActions.registerCommands));
            }
            async openFirstWindow() {
                this.restoreTheme();
                const wb = colibri.ui.ide.Workbench.getWorkbench();
                wb.addEventListener(colibri.ui.ide.EVENT_PROJECT_OPENED, e => {
                    wb.getGlobalPreferences().setValue("defaultProjectData", {
                        "projectName": wb.getFileStorage().getRoot().getName()
                    });
                });
                const prefs = wb.getGlobalPreferences();
                const defaultProjectData = prefs.getValue("defaultProjectData");
                let win = null;
                if (defaultProjectData) {
                    const projectName = defaultProjectData["projectName"];
                    let projects = await wb.getFileStorage().getProjects();
                    if (projects.indexOf(projectName) >= 0) {
                        await this.ideOpenProject(projectName);
                        return;
                    }
                }
                win = wb.activateWindow(ide_1.ui.WelcomeWindow.ID);
                if (win) {
                    win.restoreState(wb.getProjectPreferences());
                }
            }
            async ideOpenProject(projectName) {
                this._openingProject = true;
                const dlg = new ide_1.ui.dialogs.OpeningProjectDialog();
                dlg.create();
                dlg.setTitle("Opening " + projectName);
                dlg.setProgress(0);
                const monitor = new controls.dialogs.ProgressDialogMonitor(dlg);
                try {
                    const wb = colibri.ui.ide.Workbench.getWorkbench();
                    {
                        const win = wb.getActiveWindow();
                        if (win instanceof ide_1.ui.DesignWindow) {
                            win.saveState(wb.getProjectPreferences());
                        }
                    }
                    console.log(`IDEPlugin: opening project ${projectName}`);
                    await wb.openProject(projectName, monitor);
                    dlg.setProgress(1);
                    const designWindow = wb.activateWindow(ide_1.ui.DesignWindow.ID);
                    if (designWindow) {
                        designWindow.restoreState(wb.getProjectPreferences());
                    }
                }
                finally {
                    this._openingProject = false;
                    dlg.close();
                }
            }
            isOpeningProject() {
                return this._openingProject;
            }
            switchTheme() {
                const theme = controls.Controls.switchTheme();
                const prefs = colibri.ui.ide.Workbench.getWorkbench().getGlobalPreferences();
                prefs.setValue("phasereditor2d.ide.theme", {
                    theme: theme.name
                });
            }
            restoreTheme() {
                const prefs = colibri.ui.ide.Workbench.getWorkbench().getGlobalPreferences();
                const themeData = prefs.getValue("phasereditor2d.ide.theme");
                let theme = controls.Controls.LIGHT_THEME;
                if (themeData) {
                    const themeName = themeData.theme;
                    if (themeName === "dark") {
                        theme = controls.Controls.DARK_THEME;
                    }
                }
                controls.Controls.useTheme(theme);
            }
        }
        IDEPlugin._instance = new IDEPlugin();
        ide_1.IDEPlugin = IDEPlugin;
        ide.Workbench.getWorkbench().addPlugin(IDEPlugin.getInstance());
        ide_1.VER = "3.0.0";
        async function main() {
            console.log(`%c %c Phaser Editor 2D %c v${ide_1.VER} %c %c https://phasereditor2d.com `, "background-color:red", "background-color:#3f3f3f;color:whitesmoke", "background-color:orange;color:black", "background-color:red", "background-color:silver");
            colibri.ui.controls.dialogs.AlertDialog.replaceConsoleAlert();
            const wb = ide.Workbench.getWorkbench();
            await wb.launch();
            await IDEPlugin.getInstance().openFirstWindow();
        }
        window.addEventListener("load", main);
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide_2) {
        var ui;
        (function (ui) {
            var controls = colibri.ui.controls;
            var ide = colibri.ui.ide;
            class DesignWindow extends ide.WorkbenchWindow {
                constructor() {
                    super(DesignWindow.ID);
                    ide.Workbench.getWorkbench().addEventListener(ide.EVENT_PART_ACTIVATED, e => {
                        this.saveWindowState();
                    });
                }
                saveWindowState() {
                    if (ide_2.IDEPlugin.getInstance().isOpeningProject()) {
                        return;
                    }
                    const wb = ide.Workbench.getWorkbench();
                    const win = wb.getActiveWindow();
                    if (win instanceof DesignWindow) {
                        win.saveState(wb.getProjectPreferences());
                    }
                }
                saveState(prefs) {
                    this.saveEditorsState(prefs);
                }
                restoreState(prefs) {
                    this.restoreEditors(prefs);
                }
                createParts() {
                    this._outlineView = new phasereditor2d.outline.ui.views.OutlineView();
                    this._filesView = new phasereditor2d.files.ui.views.FilesView();
                    this._inspectorView = new phasereditor2d.inspector.ui.views.InspectorView();
                    this._blocksView = new phasereditor2d.blocks.ui.views.BlocksView();
                    this._editorArea = new ide.EditorArea();
                    this._split_Files_Blocks = new controls.SplitPanel(this.createViewFolder(this._filesView), this.createViewFolder(this._blocksView));
                    this._split_Editor_FilesBlocks = new controls.SplitPanel(this._editorArea, this._split_Files_Blocks, false);
                    this._split_Outline_EditorFilesBlocks = new controls.SplitPanel(this.createViewFolder(this._outlineView), this._split_Editor_FilesBlocks);
                    this._split_OutlineEditorFilesBlocks_Inspector = new controls.SplitPanel(this._split_Outline_EditorFilesBlocks, this.createViewFolder(this._inspectorView));
                    this.getClientArea().add(this._split_OutlineEditorFilesBlocks_Inspector);
                    this.initToolbar();
                    this.initialLayout();
                }
                initToolbar() {
                    const toolbar = this.getToolbar();
                    const leftArea = toolbar.getLeftArea();
                    const manager = new controls.ToolbarManager(leftArea);
                    manager.add(new phasereditor2d.files.ui.actions.OpenNewFileDialogAction());
                    manager.add(new ui.actions.OpenProjectsDialogAction());
                    manager.add(new phasereditor2d.ide.ui.actions.PlayProjectAction());
                }
                getEditorArea() {
                    return this._editorArea;
                }
                initialLayout() {
                    //const b = { x: 0, y: 0, width: window.innerWidth, height: window.innerHeight };
                    this._split_Files_Blocks.setSplitFactor(0.2);
                    this._split_Editor_FilesBlocks.setSplitFactor(0.6);
                    this._split_Outline_EditorFilesBlocks.setSplitFactor(0.15);
                    this._split_OutlineEditorFilesBlocks_Inspector.setSplitFactor(0.8);
                    //this.setBounds(b);
                    this.layout();
                }
            }
            DesignWindow.ID = "phasereditor2d.ide.ui.DesignWindow";
            ui.DesignWindow = DesignWindow;
        })(ui = ide_2.ui || (ide_2.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            class WelcomeWindow extends colibri.ui.ide.WorkbenchWindow {
                constructor() {
                    super(WelcomeWindow.ID);
                }
                getEditorArea() {
                    return new colibri.ui.ide.EditorArea();
                }
                async createParts() {
                    const dlg = new ui.dialogs.ProjectsDialog();
                    dlg.create();
                    dlg.setCloseWithEscapeKey(false);
                }
            }
            WelcomeWindow.ID = "phasereditor2d.welcome.ui.WelcomeWindow";
            ui.WelcomeWindow = WelcomeWindow;
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var actions;
            (function (actions) {
                actions.CMD_OPEN_PROJECTS_DIALOG = "phasereditor2d.ide.ui.actions.OpenProjectsDialog";
                actions.CMD_SWITCH_THEME = "phasereditor2d.ide.ui.actions.SwitchTheme";
                class IDEActions {
                    static registerCommands(manager) {
                        // open project
                        manager.addCommandHelper(actions.CMD_OPEN_PROJECTS_DIALOG);
                        manager.addHandlerHelper(actions.CMD_OPEN_PROJECTS_DIALOG, args => {
                            return args.activeWindow.getId() !== ui.WelcomeWindow.ID;
                        }, args => new actions.OpenProjectsDialogAction().run());
                        manager.addKeyBinding(actions.CMD_OPEN_PROJECTS_DIALOG, new colibri.ui.ide.commands.KeyMatcher({
                            control: true,
                            alt: true,
                            key: "P",
                            filterInputElements: false
                        }));
                        // switch theme
                        manager.addCommandHelper(actions.CMD_SWITCH_THEME);
                        manager.addHandlerHelper(actions.CMD_SWITCH_THEME, args => true, args => ide.IDEPlugin.getInstance().switchTheme());
                        manager.addKeyBinding(actions.CMD_SWITCH_THEME, new colibri.ui.ide.commands.KeyMatcher({
                            control: true,
                            key: "2",
                            filterInputElements: false
                        }));
                    }
                }
                actions.IDEActions = IDEActions;
            })(actions = ui.actions || (ui.actions = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var actions;
            (function (actions) {
                var controls = colibri.ui.controls;
                class OpenProjectsDialogAction extends controls.Action {
                    constructor() {
                        super({
                            // text: "Open Project",
                            icon: colibri.ui.ide.Workbench.getWorkbench().getWorkbenchIcon(colibri.ui.ide.ICON_FOLDER)
                        });
                    }
                    run() {
                        const dlg = new ui.dialogs.ProjectsDialog();
                        dlg.create();
                        dlg.addButton("Cancel", () => dlg.close());
                    }
                }
                actions.OpenProjectsDialogAction = OpenProjectsDialogAction;
            })(actions = ui.actions || (ui.actions = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var actions;
            (function (actions) {
                var controls = colibri.ui.controls;
                class PlayProjectAction extends controls.Action {
                    constructor() {
                        super({
                            // text: "Play Project",
                            icon: ide.IDEPlugin.getInstance().getIcon(ide.ICON_PLAY)
                        });
                    }
                    run() {
                        const element = document.createElement("a");
                        element.href = colibri.ui.ide.FileUtils.getRoot().getUrl();
                        element.target = "blank";
                        document.body.append(element);
                        element.click();
                        element.remove();
                    }
                }
                actions.PlayProjectAction = PlayProjectAction;
            })(actions = ui.actions || (ui.actions = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                var controls = colibri.ui.controls;
                class OpeningProjectDialog extends controls.dialogs.ProgressDialog {
                    create() {
                        super.create();
                        this.getDialogBackgroundElement().classList.add("DarkDialogContainer");
                    }
                }
                dialogs.OpeningProjectDialog = OpeningProjectDialog;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var dialogs;
            (function (dialogs) {
                var controls = colibri.ui.controls;
                class ProjectsDialog extends controls.dialogs.ViewerDialog {
                    constructor() {
                        super(new controls.viewers.TreeViewer());
                    }
                    async create() {
                        super.create();
                        const viewer = this.getViewer();
                        viewer.setLabelProvider(new controls.viewers.LabelProvider());
                        viewer.setCellRendererProvider(new ui.viewers.ProjectCellRendererProvider());
                        viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
                        viewer.setInput([]);
                        viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, e => this.openProject());
                        const activeWindow = colibri.ui.ide.Workbench.getWorkbench().getActiveWindow();
                        this.setTitle("Projects");
                        this.addButton("New Project", () => { });
                        {
                            const btn = this.addButton("Open Project", () => this.openProject());
                            btn.disabled = true;
                            viewer.addEventListener(controls.EVENT_SELECTION_CHANGED, e => {
                                btn.disabled = !(viewer.getSelection().length === 1);
                            });
                        }
                        let projects = await colibri.ui.ide.FileUtils.getProjects_async();
                        const root = colibri.ui.ide.FileUtils.getRoot();
                        if (root) {
                            projects = projects.filter(project => root.getName() !== project);
                        }
                        viewer.setInput(projects);
                        viewer.repaint();
                    }
                    async openProject() {
                        this.close();
                        const project = this.getViewer().getSelectionFirstElement();
                        ide.IDEPlugin.getInstance().ideOpenProject(project);
                    }
                }
                dialogs.ProjectsDialog = ProjectsDialog;
            })(dialogs = ui.dialogs || (ui.dialogs = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var ide;
    (function (ide) {
        var ui;
        (function (ui) {
            var viewers;
            (function (viewers) {
                var controls = colibri.ui.controls;
                class ProjectCellRendererProvider {
                    getCellRenderer(element) {
                        return new controls.viewers.IconImageCellRenderer(colibri.ui.ide.Workbench.getWorkbench().getWorkbenchIcon(colibri.ui.ide.ICON_FOLDER));
                    }
                    preload(element) {
                        return controls.Controls.resolveNothingLoaded();
                    }
                }
                viewers.ProjectCellRendererProvider = ProjectCellRendererProvider;
            })(viewers = ui.viewers || (ui.viewers = {}));
        })(ui = ide.ui || (ide.ui = {}));
    })(ide = phasereditor2d.ide || (phasereditor2d.ide = {}));
})(phasereditor2d || (phasereditor2d = {}));
