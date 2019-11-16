namespace phasereditor2d.files.ui.viewers {

    import io = colibri.core.io;
    import viewers = colibri.ui.controls.viewers;
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;

    export class FileCellRenderer extends viewers.IconImageCellRenderer {

        constructor() {
            super(null);
        }

        getIcon(obj: any): controls.IImage {

            const file = <io.FilePath>obj;

            if (file.isFile()) {

                const ct = ide.Workbench.getWorkbench().getContentTypeRegistry().getCachedContentType(file);

                const icon = ide.Workbench.getWorkbench().getContentTypeIcon(ct);

                if (icon) {
                    return icon;
                }

            } else {
                return controls.Controls.getIcon(ide.ICON_FOLDER);
            }

            return controls.Controls.getIcon(ide.ICON_FILE);
        }

        preload(obj: any) {

            const file = <io.FilePath>obj;

            if (file.isFile()) {
                const result = ide.Workbench.getWorkbench().getContentTypeRegistry().preload(file);
                return result
            }

            return super.preload(obj);
        }
    }
}