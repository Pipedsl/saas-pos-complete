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

  // Lógica para Eliminar Producto
  deleteProduct(product: Product) {
    this.confirmationService.confirm({
      message: `¿Estás seguro de eliminar "${product.name}"? Esta acción no se puede deshacer.`,
      header: 'Confirmar Eliminación',
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger p-button-text',
      rejectButtonStyleClass: 'p-button-text p-button-secondary',
      acceptLabel: 'Sí, eliminar',
      rejectLabel: 'Cancelar',

      accept: () => {
        // Llamada al backend
        this.productsService.deleteProduct(product.id).subscribe({
          next: () => {
            this.messageService.add({ severity: 'success', summary: 'Eliminado', detail: 'Producto borrado correctamente' });
            // Recargar la tabla para que desaparezca
            this.loadProducts();
          },
          error: (err) => {
            console.error(err);
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'No se pudo eliminar el producto' });
          }
        });
      }
    });
  }

  // Helper para calcular precio con IVA
  getPriceWithTax(p: Product): number {
    const tax = p.taxPercent || 19;
    return p.priceNeto * (1 + tax / 100);
  }
}