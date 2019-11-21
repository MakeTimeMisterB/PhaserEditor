namespace phasereditor2d.ide.ui.dialogs {

    import controls = colibri.ui.controls;

    export class ProjectsDialog extends controls.dialogs.ViewerDialog {

        constructor() {
            super(new controls.viewers.TreeViewer());
        }

        async create() {

            super.create();

            const viewer = this.getViewer();

            viewer.setLabelProvider(new controls.viewers.LabelProvider());
            viewer.setCellRendererProvider(new viewers.ProjectCellRendererProvider());
            viewer.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            viewer.setInput([]);

            viewer.addEventListener(controls.viewers.EVENT_OPEN_ITEM, e => this.openProject());

            const activeWindow = colibri.ui.ide.Workbench.getWorkbench().getActiveWindow();

            this.setTitle("Projects");

            this.addButton("New Project", () => this.openNewProjectDialog());

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

        private async openProject() {

            this.close();

            const project = this.getViewer().getSelectionFirstElement();

            IDEPlugin.getInstance().ideOpenProject(project);
        }

        private openNewProjectDialog() {

            const dlg = new NewProjectDialog();

            dlg.create();
        }
    }
}