namespace phasereditor2d.ui.controls.viewers {

    export interface ICellRendererProvider {
        getCellRenderer(element: any): ICellRenderer;
    }

}