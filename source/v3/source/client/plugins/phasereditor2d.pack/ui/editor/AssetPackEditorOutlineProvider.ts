namespace phasereditor2d.pack.ui.editor {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export class AssetPackEditorOutlineProvider extends ide.EditorViewerProvider {

        private _editor: AssetPackEditor;

        constructor(editor: AssetPackEditor) {
            super();

            this._editor = editor;
        }

        getContentProvider(): colibri.ui.controls.viewers.ITreeContentProvider {
            return new AssetPackEditorOutlineContentProvider(this._editor);
        }

        getLabelProvider(): colibri.ui.controls.viewers.ILabelProvider {
            return this._editor.getViewer().getLabelProvider();
        }

        getCellRendererProvider(): colibri.ui.controls.viewers.ICellRendererProvider {
            return this._editor.getViewer().getCellRendererProvider();
        }

        getTreeViewerRenderer(viewer: colibri.ui.controls.viewers.TreeViewer): colibri.ui.controls.viewers.TreeViewerRenderer {
            return new controls.viewers.TreeViewerRenderer(viewer);
        }

        getPropertySectionProvider(): colibri.ui.controls.properties.PropertySectionProvider {
            return null;
        }

        getInput() {
            return this._editor.getViewer().getInput();
        }

        preload(): Promise<void> {
            return Promise.resolve();
        }





    }

}