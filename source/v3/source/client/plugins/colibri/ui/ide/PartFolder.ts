/// <reference path="../controls/TabPane.ts" />

namespace colibri.ui.ide {

    export class PartFolder extends controls.TabPane {

        constructor(...classList: string[]) {
            super("PartsTabPane", ...classList);

            this.addEventListener(controls.EVENT_CONTROL_LAYOUT, (e: CustomEvent) => {
                const content = this.getSelectedTabContent();
                if (content) {
                    content.layout();
                }
            })

            this.addEventListener(controls.EVENT_TAB_CLOSED, (e: CustomEvent) => {
                const part = <Part>e.detail;

                if (part.onPartClosed()) {
                    if (this.getContentList().length === 1) {
                        Workbench.getWorkbench().setActivePart(null);
                        if (this instanceof EditorArea) {
                            Workbench.getWorkbench().setActiveEditor(null);
                        }
                    }
                } else {
                    e.preventDefault();
                }
            });

            this.addEventListener(controls.EVENT_TAB_SELECTED, (e: CustomEvent) => {
                const part = <Part>e.detail;
                Workbench.getWorkbench().setActivePart(part);
                part.onPartShown();
            });
        }

        addPart(part: Part, closeable = false): void {
            part.addEventListener(EVENT_PART_TITLE_UPDATED, (e: CustomEvent) => {
                this.setTabTitle(part, part.getTitle(), part.getIcon());
            });

            this.addTab(part.getTitle(), part.getIcon(), part, closeable);

            part.setPartFolder(this);
        }
    }

}