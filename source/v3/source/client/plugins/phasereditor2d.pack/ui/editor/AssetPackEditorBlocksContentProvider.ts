namespace phasereditor2d.pack.ui.editor {

    import controls = colibri.ui.controls;
    import io = colibri.core.io;

    export class AssetPackEditorBlocksContentProvider extends files.ui.viewers.FileTreeContentProvider {

        private _editor: AssetPackEditor;
        private _ignoreFileSet: Set<io.FilePath>;

        constructor(editor: AssetPackEditor) {
            super();

            this._editor = editor;
            this._ignoreFileSet = new Set();
        }

        getIgnoreFileSet() {
            return this._ignoreFileSet;
        }

        async updateIgnoreFileSet_async() {

            let packs = (await core.AssetPackUtils.getAllPacks())
                .filter(pack => pack.getFile() !== this._editor.getInput());

            this._ignoreFileSet = new Set();

            for (const pack of packs) {
                pack.computeUsedFiles(this._ignoreFileSet);
            }

            this._editor.getPack().computeUsedFiles(this._ignoreFileSet);
        }

        getRoots(input: any): any[] {

            return super.getRoots(input)

                .filter(obj => this.acceptFile(obj))
        }

        getChildren(parent: any): any[] {

            return super.getChildren(parent)

                .filter(obj => this.acceptFile(obj));
        }

        private acceptFile(parent: io.FilePath) {

            if (parent.isFile() && !this._ignoreFileSet.has(parent)) {

                // TODO: we should create an extension point to know 
                // what files are created by the editor and are not
                // intended to be imported in the asset pack.
                if (parent.getExtension() === "scene") {
                    return false;
                }

                return true;
            }

            for (const file of parent.getFiles()) {
                if (this.acceptFile(file)) {
                    return true;
                }
            }

            return false;
        }
    }

}