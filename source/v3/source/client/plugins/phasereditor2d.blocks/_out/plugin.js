var phasereditor2d;
(function (phasereditor2d) {
    var blocks;
    (function (blocks) {
        var ide = colibri.ui.ide;
        blocks.ICON_BLOCKS = "blocks";
        class BlocksPlugin extends ide.Plugin {
            constructor() {
                super("phasereditor2d.blocks");
            }
            static getInstance() {
                return this._instance;
            }
            registerExtensions(reg) {
                reg.addExtension(ide.IconLoaderExtension.POINT_ID, ide.IconLoaderExtension.withPluginFiles(this, [
                    blocks.ICON_BLOCKS
                ]));
            }
        }
        BlocksPlugin._instance = new BlocksPlugin();
        blocks.BlocksPlugin = BlocksPlugin;
    })(blocks = phasereditor2d.blocks || (phasereditor2d.blocks = {}));
})(phasereditor2d || (phasereditor2d = {}));
var phasereditor2d;
(function (phasereditor2d) {
    var blocks;
    (function (blocks) {
        var ui;
        (function (ui) {
            var views;
            (function (views) {
                var ide = colibri.ui.ide;
                class BlocksView extends ide.EditorViewerView {
                    constructor() {
                        super("BlocksView");
                        this.setTitle("Blocks");
                        this.setIcon(blocks.BlocksPlugin.getInstance().getIcon(blocks.ICON_BLOCKS));
                    }
                    getViewerProvider(editor) {
                        return editor.getEditorViewerProvider(BlocksView.EDITOR_VIEWER_PROVIDER_KEY);
                    }
                }
                BlocksView.EDITOR_VIEWER_PROVIDER_KEY = "Blocks";
                views.BlocksView = BlocksView;
            })(views = ui.views || (ui.views = {}));
        })(ui = blocks.ui || (blocks.ui = {}));
    })(blocks = phasereditor2d.blocks || (phasereditor2d.blocks = {}));
})(phasereditor2d || (phasereditor2d = {}));
