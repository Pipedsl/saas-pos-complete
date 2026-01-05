import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PrimeImportsModule } from '../../../../prime-imports';
import { SalesService, Sale } from '../../../../core/services/sales.service';
import { ProductsService } from '../../../../core/services/products.service';
import { Product } from '../../../../core/models/product.model';
import { MessageService } from 'primeng/api';

@Component({
    selector: 'app-sale-detail',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, PrimeImportsModule],
    providers: [MessageService],
    templateUrl: './sale-detail.html'
})
export class SaleDetailComponent implements OnInit {
    sale: Sale | null = null;
    loading = true;

    // Variables de Edición
    isEditing = false;
    tempItems: any[] = [];

    // Buscador de productos
    productSearchQuery: any = '';
    productSuggestions: Product[] = [];

    // Diálogo de confirmación
    showReasonDialog = false;
    editReason = '';

    constructor(
        private route: ActivatedRoute,
        private salesService: SalesService,
        private productsService: ProductsService,
        private messageService: MessageService,
        private cd: ChangeDetectorRef // <--- Inyección
    ) { }

    ngOnInit() {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.loadSale(id);
        }
    }

    loadSale(id: string) {
        this.loading = true;
        this.salesService.getSaleById(id).subscribe({
            next: (data) => {
                this.sale = data;
                this.loading = false;
                this.cd.detectChanges(); // <--- Actualizar vista
            },
            error: (err) => {
                this.loading = false;
                this.cd.detectChanges();
            }
        });
    }

    // --- MODO EDICIÓN ---
    toggleEdit() {
        this.isEditing = !this.isEditing;
        if (this.isEditing && this.sale) {
            // Copia profunda para editar sin afectar la vista original
            this.tempItems = JSON.parse(JSON.stringify(this.sale.items));
        }
    }

    // Buscador para agregar items
    searchProduct(event: any) {
        this.productsService.searchProducts(event.query).subscribe(data => {
            this.productSuggestions = data;
        });
    }

    addProduct(event: any) {
        const p: Product = event.value;

        // Verificar duplicados en la lista temporal
        const existing = this.tempItems.find(i => (i.product && i.product.id === p.id) || i.productId === p.id);

        if (existing) {
            existing.quantity++;
            existing.total = existing.unitPrice * existing.quantity;
            this.messageService.add({ severity: 'info', summary: 'Agregado', detail: 'Cantidad aumentada' });
        } else {
            this.tempItems.push({
                product: p,      // Para mostrar nombre en tabla
                productId: p.id, // Para el backend
                quantity: 1,
                unitPrice: p.priceFinal,
                total: p.priceFinal
            });
        }

        // Limpiar buscador
        this.productSearchQuery = '';
    }

    // Modificar cantidades (+ / -)
    updateQty(index: number, delta: number) {
        const item = this.tempItems[index];
        const newQty = item.quantity + delta;

        if (newQty > 0) {
            item.quantity = newQty;
            item.total = item.unitPrice * newQty;
        }
    }

    removeItem(index: number) {
        this.tempItems.splice(index, 1);
    }

    calculateTotal() {
        return this.tempItems.reduce((acc, item) => acc + item.total, 0);
    }

    // Guardar Cambios
    confirmSave() {
        if (!this.editReason.trim()) return;
        if (!this.sale) return;

        this.salesService.updateSale(this.sale.id, this.tempItems, this.editReason).subscribe({
            next: (updatedSale) => {
                this.sale = updatedSale;
                this.isEditing = false;
                this.showReasonDialog = false;
                this.editReason = ''; // Limpiar motivo

                this.messageService.add({ severity: 'success', summary: 'Ticket Actualizado', detail: 'Inventario ajustado correctamente' });
                this.cd.detectChanges(); // <--- Importante
            },
            error: (err) => {
                console.error(err);
                this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo editar el ticket' });
                this.cd.detectChanges();
            }
        });
    }
}