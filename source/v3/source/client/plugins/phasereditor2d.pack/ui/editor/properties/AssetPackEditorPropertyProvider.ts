namespace phasereditor2d.pack.ui.editor.properties {

    import controls = colibri.ui.controls;

    export class AssetPackEditorPropertyProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {

            sections.push(new ItemSection(page));

            sections.push(new ImageSection(page));

            sections.push(new SVGSection(page));

            sections.push(new AtlasSection(page));

            sections.push(new AtlasXMLSection(page));

            sections.push(new UnityAtlasSection(page));

            sections.push(new MultiatlasSection(page));

            sections.push(new SpritesheetSection(page));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.AnimationsSection",
                "Animations",
                "URL",
                "url",
                core.contentTypes.CONTENT_TYPE_ANIMATIONS,
                core.ANIMATIONS_TYPE));

            sections.push(new BitmapFontSection(page));

            sections.push(new TilemapCSVSection(page));

            sections.push(new TilemapImpactSection(page));

            sections.push(new TilemapTiledJSONSection(page));

            sections.push(new PluginSection(page));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.SceneFileSection",
                "Scene File",
                "URL",
                "url",
                files.core.CONTENT_TYPE_JAVASCRIPT,
                core.SCENE_FILE_TYPE));

            sections.push(new ScenePluginSection(page));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.ScriptSection",
                "Script",
                "URL",
                "url",
                files.core.CONTENT_TYPE_JAVASCRIPT,
                core.SCRIPT_TYPE));

            sections.push(new AudioSection(page));

            sections.push(new AudioSpriteSection(page));

            sections.push(new VideoSection(page));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.TextSection",
                "Text",
                "URL",
                "url",
                files.core.CONTENT_TYPE_TEXT,
                core.TEXT_TYPE));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.CSSSection",
                "CSS",
                "URL",
                "url",
                files.core.CONTENT_TYPE_CSS,
                core.CSS_TYPE));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.GLSLSection",
                "GLSL",
                "URL",
                "url",
                files.core.CONTENT_TYPE_GLSL,
                core.GLSL_TYPE));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.HTMLSection",
                "HTML",
                "URL",
                "url",
                files.core.CONTENT_TYPE_HTML,
                core.HTML_TYPE));

            sections.push(new HTMLTextureSection(page));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.BinarySection",
                "Binary",
                "URL",
                "url",
                colibri.core.CONTENT_TYPE_ANY,
                core.BINARY_TYPE));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.JSONSection",
                "JSON",
                "URL",
                "url",
                files.core.CONTENT_TYPE_JSON,
                core.JSON_TYPE));

            sections.push(new SimpleURLSection(page,
                "phasereditor2d.pack.ui.editor.properties.XMLSection",
                "XML",
                "URL",
                "url",
                files.core.CONTENT_TYPE_XML,
                core.XML_TYPE));

            // preview sections

            sections.push(new ui.properties.ImagePreviewSection(page));

            sections.push(new ui.properties.ManyImageSection(page));
        }

    }
}