
namespace phasereditor2d.images {

    import ide = colibri.ui.ide;
    import controls = colibri.ui.controls;

    export class ImagesPlugin extends ide.Plugin {

        private static _instance = new ImagesPlugin();

        static getInstance() {
            return this._instance;
        }

        private constructor() {
            super("phasereditor2d.images");
        }

        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry) {

            // file cell renderers

            reg
                .addExtension(
                    new files.ui.viewers.SimpleContentTypeCellRendererExtension(
                        webContentTypes.core.CONTENT_TYPE_IMAGE,
                        new ui.viewers.ImageFileCellRenderer())
                );

            reg
                .addExtension(
                    new files.ui.viewers.SimpleContentTypeCellRendererExtension(
                        webContentTypes.core.CONTENT_TYPE_SVG,
                        new ui.viewers.ImageFileCellRenderer())
                );

            // editors

            reg.addExtension(
                new ide.EditorExtension([ui.editors.ImageEditor.getFactory()]));
        }

    }

    ide.Workbench.getWorkbench().addPlugin(ImagesPlugin.getInstance());
}