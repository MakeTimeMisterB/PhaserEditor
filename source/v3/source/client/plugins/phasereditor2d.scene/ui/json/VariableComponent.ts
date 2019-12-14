namespace phasereditor2d.scene.ui.json {

    import write = colibri.core.json.write;
    import read = colibri.core.json.read;

    export class VariableComponent {

        static write(sprite: gameobjects.EditorImage, data: any): void {
            write(data, "label", sprite.getEditorLabel());
        }

        static read(sprite: gameobjects.EditorImage, data: any): void {
            sprite.setEditorLabel(read(data, "label"));
        }

    }

}