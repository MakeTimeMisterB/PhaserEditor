namespace phasereditor2d.pack.ui.editor {

    import ide = colibri.ui.ide;

    export class AssetPackEditorOperation extends ide.undo.Operation {

        private _editor: AssetPackEditor;
        private _before: any;
        private _after: any;

        static takeSnapshot(editor: AssetPackEditor): any {
            return editor.getPack().toJSON();
        }

        constructor(editor: AssetPackEditor, before: any, after: any) {
            super();

            this._editor = editor;
            this._before = before;
            this._after = after;
        }

        private load(data : any) {
            this._editor.getPack().fromJSON(data);
            this._editor.updateAll();
        }

        undo(): void {
            this.load(this._before);
        }

        redo(): void {
            this.load(this._after);
        }
    }
}