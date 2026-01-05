import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
// Tu módulo compartido de PrimeNG



import { MessageService } from 'primeng/api';
import { PrimeImportsModule } from '../../../prime-imports';
import { ReportsService, InventoryLog } from '../../../core/services/reports.service';
import { CategoriesService } from '../../../core/services/categories.service';
import { Category } from '../../../core/models/category.model';
import { DatePicker } from "primeng/datepicker";
import { Select } from "primeng/select";

@Component({
    selector: 'app-inventory-history',
    standalone: true,
    imports: [CommonModule, FormsModule, PrimeImportsModule, DatePicker, Select],
    providers: [MessageService],
    templateUrl: './inventory-history.html',
})
export class InventoryHistoryComponent implements OnInit {

    logs: InventoryLog[] = [];
    categories: Category[] = [];
    loading = false;

    // Filtros
    rangeDates: Date[] | undefined; // Array para el rango del calendario
    selectedCategory: Category | null = null;

    constructor(
        private reportsService: ReportsService,
        private categoriesService: CategoriesService,
        private messageService: MessageService,
        private cd: ChangeDetectorRef
    ) { }

    ngOnInit() {
        this.loadCategories();
        this.loadLogs(); // Carga inicial (mes actual por defecto en el backend)
    }

    loadCategories() {
        // Si aún no tienes CategoriesService implementado, puedes comentar esto o crear un mock
        this.categoriesService.getCategories().subscribe({
            next: (cats) => this.categories = cats,
            error: () => console.warn('No se pudieron cargar categorías')
        });
    }

    loadLogs() {
        this.loading = true;

        let startStr: string | undefined;
        let endStr: string | undefined;

        if (this.rangeDates && this.rangeDates[0]) {
            startStr = this.rangeDates[0].toISOString();
            // Si el usuario seleccionó fecha fin, la usamos. Si no, usamos la de inicio (búsqueda de 1 día)
            endStr = (this.rangeDates[1] || this.rangeDates[0]).toISOString();
        }

        const catId = this.selectedCategory?.id;

        this.reportsService.getInventoryLogs(startStr, endStr, catId).subscribe({
            next: (data) => {
                this.logs = data;
                this.loading = false;
                this.cd.detectChanges();
            },
            error: (err) => {
                console.error(err);
                this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo cargar el historial' });
                this.loading = false;
                this.cd.detectChanges();
            }
        });
    }

    downloadCsv() {
        let startStr: string | undefined;
        let endStr: string | undefined;

        if (this.rangeDates && this.rangeDates[0]) {
            startStr = this.rangeDates[0].toISOString();
            endStr = (this.rangeDates[1] || this.rangeDates[0]).toISOString();
        }

        this.reportsService.downloadCsv(startStr, endStr, this.selectedCategory?.id).subscribe((blob) => {
            // Truco para descargar el archivo desde el navegador
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `reporte_inventario_${new Date().getTime()}.csv`;
            a.click();
            window.URL.revokeObjectURL(url);
        });
    }

    // Colores para las etiquetas de acción
    getActionSeverity(action: string) {
        switch (action) {
            case 'CREATE': return 'success';
            case 'SALE': return 'info';
            case 'WEB_ORDER': return 'info';
            case 'SALE_EDIT_RETURN': return 'warn';
            case 'SALE_EDIT_OUT': return 'warn';
            case 'MANUAL_UPDATE': return 'secondary';
            case 'RETURN': return 'danger';
            default: return 'secondary'; // Cambiado 'contrast' por 'secondary' si usas una versión antigua de PrimeNG
        }
    }
}