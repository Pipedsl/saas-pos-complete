import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductsService } from '../../../core/services/products.service';
import { CartService } from '../services/cart.service';
import { Product } from '../../../core/models/product.model';
import { CartItem } from '../../../core/models/cart.model';
import { Observable } from 'rxjs';
import { PrimeImportsModule } from '../../../prime-imports';
import { FormsModule } from '@angular/forms'; // Necesario para ngModel
import { MessageService } from 'primeng/api';
import { InputNumber } from 'primeng/inputnumber';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Category } from '../../../core/models/category.model';
import { CategoriesService } from '../../../core/services/categories.service';
import { BarcodeScannerComponent } from '../../../shared/components/barcode-scanner/barcode-scanner';


@Component({
  selector: 'app-pos',
  imports: [CommonModule, PrimeImportsModule, FormsModule, BarcodeScannerComponent],
  providers: [MessageService],
  templateUrl: './pos.html',
  styleUrl: './pos.css',
})
export class PosComponent implements OnInit {

  // UI Estado
  mobileView: 'CATALOG' | 'CART' = 'CATALOG';
  searchTerm: string = '';
  loading = false;

  // Datos
  allProducts: Product[] = [];
  visibleProducts: Product[] = [];
  cartItems$: Observable<CartItem[]>;
  total$: Observable<number>;

  //Categorías
  categories: Category[] = [];
  selectedCategoryId: string | null | undefined = null; //null significa "todas"

  // --- MODAL CANTIDAD ---
  showQtyDialog: boolean = false;
  selectedProduct: Product | null = null;
  qtyToAdd: number = 1;
  @ViewChild('qtyInput') qtyInput!: InputNumber;

  // --- MODAL PAGO ---
  showPaymentDialog = false;
  paymentMethod: 'CASH' | 'CARD' | 'TRANSFER' = 'CASH';
  amountReceived: number | null = null;
  changeAmount: number = 0;
  currentTotal: number = 0;

  // --- CONFIGURACIÓN E IMPRESIÓN ---
  shopSettings: any = {};
  lastSaleTicket: any = null;
  @ViewChild('ticketContent') ticketContentRef!: ElementRef;

  // --- CÓDIGO ---
  showScanner: boolean = false;

  constructor(
    private productsService: ProductsService,
    private cartService: CartService,
    private cd: ChangeDetectorRef,
    private messageService: MessageService,
    private http: HttpClient,
    private categoryService: CategoriesService
  ) {
    this.cartItems$ = this.cartService.items$;
    this.total$ = this.cartService.total$;
  }

  ngOnInit() {
    this.loadProducts();
    this.loadSettings();
    this.loadCategories();
  }

  handleScan(code: string) {
    console.log('Producto escaneado:', code);

    // 1. Cerrar la cámara
    this.showScanner = false;

    // 2. Poner el código en el buscador visualmente
    this.searchTerm = code;

    // 3. Ejecutar la búsqueda
    this.onSearch(code);

    // 4. Intentar seleccionar el producto automáticamente
    // Usamos un pequeño timeout para asegurar que el filtro (onSearch) terminó
    setTimeout(() => {
      // Buscamos coincidencia EXACTA por SKU
      const foundProduct = this.visibleProducts.find(p => p.sku.toLowerCase() === code.toLowerCase());

      if (foundProduct) {
        // Si lo encontramos, abrimos el modal de agregar (igual que si le hubieras hecho clic)
        this.selectProduct(foundProduct);
        this.messageService.add({ severity: 'success', summary: 'Encontrado', detail: foundProduct.name, life: 1000 });
      } else {
        this.showError('No encontrado', `No existe producto con SKU: ${code}`);
      }
    }, 200);
  }

  loadSettings() {
    this.http.get(`${environment.apiUrl}/api/settings`).subscribe({
      next: (data: any) => this.shopSettings = data || {},
      error: () => this.shopSettings = {} // Fallback silencioso
    });
  }

  toggleMobileView() {
    this.mobileView = this.mobileView === 'CATALOG' ? 'CART' : 'CATALOG';
  }

  loadCategories() {
    this.categoryService.getCategories().subscribe(
      data => this.categories = data
    );
  }

  applyFilters() {
    let filtered = this.allProducts;

    if (this.selectedCategoryId) {
      filtered = filtered.filter(p => p.categoryId === this.selectedCategoryId);
    }

    if (this.searchTerm.trim()) {
      const lowerTerm = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p =>
        p.name.toLowerCase().includes(lowerTerm) ||
        p.sku.toLowerCase().includes(lowerTerm)
      );
    }

    this.visibleProducts = filtered;
  }

  filterByCategory(categoryId: string | null | undefined) {
    this.selectedCategoryId = categoryId;
    this.applyFilters();
  }


  loadProducts() {
    this.productsService.getProducts().subscribe({
      next: (data) => {
        this.allProducts = [...data];
        this.visibleProducts = [...data];
        this.cd.detectChanges();
      },
      error: (err) => console.error(err)
    });
  }

  // --- BÚSQUEDA ---
  onSearch(term: string) {
    this.searchTerm = term;
    this.applyFilters();
  }

  onEnterSearch() {
    if (this.visibleProducts.length === 1) {
      this.selectProduct(this.visibleProducts[0]);
    }
  }

  // --- LÓGICA AGREGAR (MODAL) ---
  selectProduct(product: Product) {
    if (product.stockCurrent <= 0) {
      this.showError('Sin Stock', `El producto ${product.name} está agotado.`);
      return;
    }
    this.selectedProduct = product;
    this.qtyToAdd = product.measurementUnit === 'KG' ? 1 : 1;
    this.showQtyDialog = true;
  }

  focusQtyInput() {
    setTimeout(() => {
      if (this.qtyInput && this.qtyInput.input && this.qtyInput.input.nativeElement) {
        this.qtyInput.input.nativeElement.focus();
        this.qtyInput.input.nativeElement.select();
      }
    }, 100);
  }

  confirmAddToCart() {
    if (!this.selectedProduct || this.qtyToAdd <= 0) return;
    const success = this.cartService.addToCart(this.selectedProduct, this.qtyToAdd);

    if (!success) {
      this.showError('Stock Insuficiente', `No puedes superar el stock disponible.`);
    } else {
      this.showQtyDialog = false;
      this.searchTerm = '';
      this.visibleProducts = this.allProducts;
      this.cd.detectChanges();
    }
  }

  // --- ACCIONES DEL CARRITO ---
  increaseItem(item: CartItem) {
    const success = this.cartService.addToCart(item.product, 1);
    if (!success) this.showError('Tope de Stock', 'No queda más stock.');
  }

  decreaseItem(productId: string) {
    this.cartService.decreaseQuantity(productId);
  }

  deleteItem(productId: string) {
    this.cartService.removeFromCart(productId);
  }

  clearCart() {
    this.cartService.clearCart();
  }

  // --- PROCESO DE PAGO ---
  initiateCheckout() {
    const items = this.cartService.getCurrentItems();
    if (items.length === 0) {
      this.showError('Carrito Vacío', 'Agrega productos antes de cobrar.');
      return;
    }

    this.currentTotal = this.cartService.getCurrentTotal();
    this.amountReceived = null;
    this.changeAmount = 0;
    this.paymentMethod = 'CASH';
    this.showPaymentDialog = true;
  }

  calculateChange() {
    if (this.amountReceived && this.amountReceived >= this.currentTotal) {
      this.changeAmount = this.amountReceived - this.currentTotal;
    } else {
      this.changeAmount = 0;
    }
  }

  finalizeSale() {
    // 1. Validación de efectivo
    if (this.paymentMethod === 'CASH' && (this.amountReceived ?? 0) < this.currentTotal) {
      this.showError('Pago Insuficiente', 'El monto recibido es menor al total.');
      return;
    }

    this.loading = true;

    // 2. Llamada al Backend
    this.cartService.processSale(this.paymentMethod).subscribe({
      next: (res) => {
        this.loading = false;
        this.showPaymentDialog = false;

        // 3. Guardar datos para impresión (Importante para el ticket)
        this.lastSaleTicket = {
          saleNumber: res.saleNumber,
          createdAt: new Date(),
          items: this.cartService.getCurrentItems(), // Guardamos copia de items antes de limpiar
          total: this.currentTotal,
          change: this.changeAmount,
          amountReceived: this.amountReceived, // Guardamos cuánto pagó
          paymentMethod: this.paymentMethod
        };

        // 4. Mandar a imprimir
        setTimeout(() => {
          this.printTicketHardcore();

          // 5. Limpieza final
          this.cartService.clearCart();
          this.loadProducts();
          this.messageService.add({ severity: 'success', summary: 'Venta Exitosa', detail: `Ticket #${res.saleNumber}` });
        }, 500);
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
        const msg = err.error || 'Error al procesar venta.';
        this.showError('Error', msg);
      }
    });
  }

  // --- IMPRESIÓN DE TICKET (CORREGIDO CON EFECTIVO/VUELTO) ---
  private printTicketHardcore() {
    // Datos de configuración
    const shopName = this.shopSettings.shopName || 'MI NEGOCIO';
    const shopRut = this.shopSettings.shopRut || '';
    const shopAddress = this.shopSettings.shopAddress || '';
    const footerMsg = this.shopSettings.footerMessage || 'Gracias por su compra';
    const printerType = this.shopSettings.printerType || '58mm';

    // Estilos dinámicos
    let cssWidth = '58mm';
    let fontSize = '12px';
    let margin = '0';

    if (printerType === '80mm') { cssWidth = '80mm'; fontSize = '14px'; }
    if (printerType === 'LETTER') { cssWidth = '100%'; fontSize = '12pt'; margin = '20mm'; }

    // Formateadores
    const fmtMoney = (amount: number) => new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP' }).format(amount);
    const dateStr = new Date().toLocaleString('es-CL');

    // Construir filas de productos
    let itemsHtml = '';
    this.lastSaleTicket.items.forEach((item: any) => {
      itemsHtml += `
            <div style="display: flex; justify-content: space-between; margin-bottom: 2px;">
                <span>${item.quantity} x ${item.product.name.substring(0, 20)}</span>
                <span>${fmtMoney(item.subtotal)}</span>
            </div>
        `;
    });

    // Construir sección de pago (AQUÍ ESTABA LO QUE FALTABA)
    let paymentHtml = '';
    if (this.lastSaleTicket.paymentMethod === 'CASH') {
      paymentHtml = `
            <div style="display: flex; justify-content: space-between; font-size: 0.9em; margin-top: 5px;">
                <span>Efectivo:</span>
                <span>${fmtMoney(this.lastSaleTicket.amountReceived || 0)}</span>
            </div>
            <div style="display: flex; justify-content: space-between; font-size: 0.9em;">
                <span>Vuelto:</span>
                <span>${fmtMoney(this.lastSaleTicket.change)}</span>
            </div>
        `;
    } else {
      paymentHtml = `
            <div style="text-align: center; font-size: 0.9em; margin-top: 5px;">
                Pago con: ${this.lastSaleTicket.paymentMethod === 'CARD' ? 'TARJETA' : 'TRANSFERENCIA'}
            </div>
        `;
    }

    // Abrir ventana popup
    const popupWin = window.open('', '_blank', 'top=0,left=0,height=600,width=400');

    if (popupWin) {
      popupWin.document.open();
      popupWin.document.write(`
        <html>
          <head>
            <title>Ticket</title>
            <style>
              @page { size: auto; margin: 0mm; }
              body {
                  font-family: 'Courier New', Courier, monospace;
                  font-size: ${fontSize};
                  width: ${cssWidth};
                  margin: ${margin};
                  padding: ${printerType === 'LETTER' ? '0' : '5px'};
                  color: black;
                  background: white;
              }
              .text-center { text-align: center; }
              .bold { font-weight: bold; }
              .divider { border-bottom: 1px dashed black; margin: 5px 0; }
              .total-row { display: flex; justify-content: space-between; font-weight: bold; font-size: 1.2em; margin-top: 5px; }
            </style>
          </head>
          <body onload="window.print(); window.onafterprint = function(){ window.close(); }">
            
            <div class="text-center">
                <h2 style="margin:0">${shopName}</h2>
                ${shopRut ? `<p style="margin:2px">${shopRut}</p>` : ''}
                ${shopAddress ? `<p style="margin:2px">${shopAddress}</p>` : ''}
                <br/>
                <p style="margin:2px">Ticket: ${this.lastSaleTicket.saleNumber}</p>
                <p style="margin:2px">${dateStr}</p>
            </div>

            <div class="divider"></div>
            ${itemsHtml}
            <div class="divider"></div>

            <div class="total-row">
                <span>TOTAL</span>
                <span>${fmtMoney(this.lastSaleTicket.total)}</span>
            </div>

            ${paymentHtml} <div class="text-center" style="margin-top: 15px; font-size: 0.8em;">
                <p>${footerMsg}</p>
            </div>

          </body>
        </html>
      `);
      popupWin.document.close();
    } else {
      alert('Permite ventanas emergentes para imprimir.');
    }
  }

  // Helpers
  getPriceWithTax(p: Product): number {
    return p.priceNeto * (1 + (p.taxPercent || 19) / 100);
  }

  showError(summary: string, detail: string) {
    this.messageService.add({ severity: 'error', summary, detail, life: 3000 });
  }
}