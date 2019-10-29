
namespace phasereditor2d.scene.ui.blocks {

    import controls = colibri.ui.controls;

    export class SceneEditorBlocksPropertyProvider extends controls.properties.PropertySectionProvider {

        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void {

            sections.push(new pack.ui.properties.AssetPackItemSection(page));
            sections.push(new pack.ui.properties.ImagePreviewSection(page));
            sections.push(new pack.ui.properties.ManyImageSection(page));

        }

    }

}