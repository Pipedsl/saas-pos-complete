import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminOrdersService, WebOrder } from '../services/admin-orders.service';
import { MessageService } from 'primeng/api';
import { PrimeImportsModule } from '../../../prime-imports';
import { AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { Product } from '../../../core/models/product.model';
import { ProductsService } from '../../../core/services/products.service';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment'; // <--- IMPORTANTE

@Component({
  selector: 'app-web-order-detail',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    PrimeImportsModule
  ],
  providers: [MessageService],
  templateUrl: './web-order-detail.html'
})
export class WebOrderDetailComponent implements OnInit {
  order: WebOrder | null = null;
  loading = true;

  isEditingItems = false;
  productSearchQuery: any = '';
  productSuggestions: Product[] = [];
  tempItems: any[] = [];

  statuses = [
    { label: 'Pendiente', value: 'PENDING' },
    { label: 'Confirmado / Pagado', value: 'PAID' },
    { label: 'En Preparación', value: 'PREPARING' },
    { label: 'Enviado / Retirado', value: 'SHIPPED' },
    { label: 'Cancelado', value: 'CANCELLED' }
  ];

  filteredStatuses: any[] = [];
  selectedStatusObj: any = null;

  // --- VARIABLES PIN ---
  showPinDialog = false;
  adminPin = '';
  pendingAction: Function | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private orderService: AdminOrdersService,
    private messageService: MessageService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
    private productsService: ProductsService
  ) { }

  ngOnInit() {
    const orderNumber = this.route.snapshot.paramMap.get('orderNumber');
    if (orderNumber) {
      this.loadOrder(orderNumber);
    }
  }

  loadOrder(orderNumber: string) {
    this.loading = true;
    this.orderService.getOrderByNumber(orderNumber).subscribe({
      next: (data) => {
        this.order = data;
        this.selectedStatusObj = this.statuses.find(s => s.value === data.status);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se encontró el pedido' });
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleEditMode() {
    this.isEditingItems = !this.isEditingItems;
    if (this.isEditingItems) {
      // Clonar items y preparar para edición
      this.tempItems = this.order?.items.map(item => {
        const currentStockInDb = item.product?.stockCurrent || 0;
        const myQuantity = item.quantity;

        return {
          ...item,
          maxStock: currentStockInDb + myQuantity,
          // Inicializamos customPrice con el precio actual para poder editarlo
          customPrice: item.unitPriceAtMoment
        };
      }) || [];
    }
  }

  filterStatus(event: any) {
    const query = event.query.toLowerCase();
    this.filteredStatuses = this.statuses.filter(stat =>
      stat.label.toLowerCase().includes(query)
    );
  }

  searchProduct(event: any) {
    this.productsService.searchProducts(event.query).subscribe(data => {
      this.productSuggestions = data;
    });
  }

  addProductToOrder(event: any) {
    const product: Product = event.value;

    if (product.stockCurrent < 1) {
      this.messageService.add({ severity: 'error', summary: 'Sin Stock', detail: `No queda stock de ${product.name}` });
      setTimeout(() => { this.productSearchQuery = null; }, 0);
      return;
    }

    const existing = this.tempItems.find(i =>
      (i.product && i.product.id === product.id) || (i.productId === product.id)
    );

    if (existing) {
      if (existing.quantity + 1 > existing.maxStock) {
        this.messageService.add({ severity: 'warn', summary: 'Límite alcanzado', detail: `Solo hay ${existing.maxStock} unidades.` });
      } else {
        existing.quantity++;
        this.recalculateSubtotal(existing);
        this.messageService.add({ severity: 'info', summary: 'Agregado', detail: 'Cantidad actualizada' });
      }
    } else {
      this.tempItems.push({
        productId: product.id,
        productNameSnapshot: product.name,
        skuSnapshot: product.sku,
        quantity: 1,
        unitPriceAtMoment: product.priceFinal,
        customPrice: product.priceFinal, // Precio inicial editable
        subtotal: product.priceFinal,
        maxStock: product.stockCurrent
      });
    }

    setTimeout(() => { this.productSearchQuery = null; }, 0);
  }

  removeItem(index: number) {
    this.tempItems.splice(index, 1);
  }

  updateQuantity(index: number, change: number) {
    const item = this.tempItems[index];
    const newQty = item.quantity + change;

    if (newQty > item.maxStock) {
      this.messageService.add({ severity: 'warn', summary: 'Stock Insuficiente', detail: `Máximo disponible: ${item.maxStock}` });
      return;
    }

    if (newQty > 0) {
      item.quantity = newQty;
      this.recalculateSubtotal(item);
    }
  }

  // Nuevo helper para recalcular subtotal al cambiar precio o cantidad
  recalculateSubtotal(item: any) {
    const price = item.customPrice !== undefined ? item.customPrice : item.unitPriceAtMoment;
    item.subtotal = price * item.quantity;
  }

  calculateTempTotal(): number {
    return this.tempItems.reduce((acc, item) => acc + (item.subtotal || 0), 0);
  }

  // --- LÓGICA DE SEGURIDAD (PIN) ---

  initiateSave() {
    const user = this.authService.getCurrentUser();
    const role = user ? user.role : 'CASHIER';

    // Si es CAJERO, pedimos PIN. Si es ADMIN, guardamos directo.
    if (role === 'CASHIER') {
      this.adminPin = '';
      this.pendingAction = () => this.executeSaveOrderItems();
      this.showPinDialog = true;
    } else {
      this.executeSaveOrderItems();
    }
  }

  verifyPinAndExecute() {
    if (!this.adminPin) return;

    this.http.post(`${environment.apiUrl}/api/settings/verify-pin`, { pin: this.adminPin })
      .subscribe({
        next: () => {
          this.showPinDialog = false;
          this.messageService.add({ severity: 'success', summary: 'Autorizado' });
          if (this.pendingAction) this.pendingAction();
        },
        error: () => {
          this.messageService.add({ severity: 'error', summary: 'Acceso Denegado', detail: 'PIN Incorrecto' });
          this.adminPin = '';
        }
      });
  }

  // Renombrado de saveOrderItems -> executeSaveOrderItems
  executeSaveOrderItems() {
    if (!this.order) return;

    const payload = this.tempItems.map(item => ({
      productId: item.product?.id || item.productId,
      quantity: item.quantity,
      customPrice: item.customPrice // <--- IMPORTANTE: Enviamos el precio editado
    }));

    this.orderService.updateOrderItems(this.order.orderNumber, payload).subscribe({
      next: (updatedOrder) => {
        this.order = updatedOrder;
        this.isEditingItems = false;
        this.messageService.add({ severity: 'success', summary: 'Guardado', detail: 'Pedido actualizado' });
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo actualizar' });
      }
    });
  }

  onStatusSelect(event: AutoCompleteSelectEvent) {
    if (!this.order || !event.value) return;
    const newStatus = event.value.value;
    const oldStatus = this.order.status;

    this.orderService.updateStatus(this.order.orderNumber, newStatus).subscribe({
      next: (updatedOrder) => {
        this.order = updatedOrder;
        this.selectedStatusObj = this.statuses.find(s => s.value === updatedOrder.status);
        this.messageService.add({ severity: 'success', summary: 'Actualizado', detail: 'Estado modificado' });
        this.cdr.detectChanges();
      },
      error: (err) => {
        const errorMsg = err.error?.error || 'No se pudo actualizar el estado';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMsg });
        this.selectedStatusObj = this.statuses.find(s => s.value === oldStatus);
        this.cdr.detectChanges();
      }
    });
  }

  getSeverity(status: string) {
    switch (status) {
      case 'PENDING': return 'warn';
      case 'PAID': return 'success';
      case 'PREPARING': return 'info';
      case 'SHIPPED': return 'success';
      case 'CANCELLED': return 'danger';
      default: return 'secondary';
    }
  }
}