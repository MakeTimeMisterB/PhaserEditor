namespace colibri.ui.ide.commands {

    export class CommandArgs {
        constructor(
            public readonly activePart: Part,
            public readonly activeEditor: EditorPart,
            public readonly activeElement: HTMLElement,
            public readonly activeWindow: ide.WorkbenchWindow,
            public readonly activeDialog: controls.dialogs.Dialog
        ) {

        }
    }

}