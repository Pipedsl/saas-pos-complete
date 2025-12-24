import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product } from '../../../core/models/product.model';
import { ProductsService } from '../../../core/services/products.service';
import { ConfirmationService, MessageService } from 'primeng/api'; // Servicios
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast'; // Importante para las notificaciones
import { PrimeImportsModule } from '../../../prime-imports';
import { RouterModule } from '@angular/router'; // Para routerLink
import { FormsModule } from '@angular/forms';
import { Category } from '../../../core/models/category.model';
import { CategoriesService } from '../../../core/services/categories.service';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    CommonModule,
    PrimeImportsModule,
    ConfirmDialogModule,
    ToastModule,
    RouterModule,
    FormsModule
  ],
  providers: [ConfirmationService, MessageService], // <--- IMPORTANTE: Proveedores locales
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductListComponent implements OnInit {
  allProducts: Product[] = [];
  products: Product[] = [];
  loading: boolean = true;

  categories: Category[] = [];
  filteredCategories: Category[] = [];
  selectedCategory: Category | null = null;

  showDeleteDialog = false;
  productToDelete: any = null;

  constructor(
    private productsService: ProductsService,
    private cd: ChangeDetectorRef,
    private confirmationService: ConfirmationService,
    private messageService: MessageService,
    private categoryService: CategoriesService
  ) { }

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
  }
  loadCategories() {
    this.categoryService.getCategories().subscribe(
      data => this.categories = data,
    );
  }

  loadProducts() {
    this.loading = true;
    this.productsService.getProducts().subscribe({
      next: (data) => {
        this.allProducts = data;
        this.products = data;
        this.loading = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando productos', err);
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  filterCategories(event: AutoCompleteCompleteEvent) {
    const query = event.query.toLowerCase();
    this.filteredCategories = this.categories.filter(category => category.name.toLowerCase().includes(query));
  }

  filterProducts() {
    if (!this.selectedCategory) {
      this.products = this.allProducts;
    } else {
      this.products = this.allProducts.filter(p => p.categoryId === this.selectedCategory!.id);
    }
  }

  deleteProduct(product: any) {
    this.productToDelete = product;
    this.showDeleteDialog = true; // Abre nuestro diálogo personalizado
  }

  confirmDelete(force: boolean) {
    if (!this.productToDelete) return;

    this.loading = true; // Opcional, si quieres mostrar spinner

    this.productsService.deleteProduct(this.productToDelete.id, force).subscribe({
      next: (res) => {
        this.messageService.add({
          severity: 'success',
          summary: 'Éxito',
          detail: force ? 'Producto y ventas eliminados' : 'Producto archivado'
        });
        this.loadProducts(); // Recargar tabla
        this.showDeleteDialog = false;
        this.productToDelete = null;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo realizar la acción' });
        this.loading = false;
      }
    });
  }

  // Helper para calcular precio con IVA
  getPriceWithTax(p: Product): number {
    const tax = p.taxPercent || 19;
    return p.priceNeto * (1 + tax / 100);
  }

  restoreProduct(product: Product) {
    this.loading = true;
    // Asumiendo que agregaste 'activateProduct' en tu service, 
    // si no, usa una llamada http directa o agrégalo al service.
    this.productsService.activateProduct(product.id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Restaurado',
          detail: `El producto ${product.name} está activo nuevamente`
        });
        product.isActive = true; // Actualizamos la vista inmediatamente
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo reactivar el producto'
        });
        this.loading = false;
      }
    });
  }
}