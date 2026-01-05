import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { PublicProduct, StoreService } from '../../../../core/services/store.service';
import { ActivatedRoute } from '@angular/router';
import { CartService } from '../../../../core/services/cart.service';
import { MessageService } from 'primeng/api';
import { PrimeImportsModule } from '../../../../prime-imports';

@Component({
  selector: 'app-catalog',
  imports: [CommonModule, PrimeImportsModule],
  templateUrl: './catalog.html',
  styleUrl: './catalog.css',
})
export class CatalogComponent implements OnInit {
  products: PublicProduct[] = [];
  loading = true;
  slug: string = '';

  constructor(
    private route: ActivatedRoute,
    private storeService: StoreService,
    public cartService: CartService, // Poner PUBLIC para usarlo en el HTML
    private messageService: MessageService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.route.parent?.paramMap.subscribe(params => {
      this.slug = params.get('slug') || '';
      if (this.slug) {
        this.loadProducts();
      }
    });
  }

  loadProducts() {
    this.loading = true;
    this.storeService.getProducts(this.slug).subscribe({
      next: (data) => {
        this.products = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onImageError(event: any) {
    event.target.src = 'https://placehold.co/600x600/f3f4f6/9ca3af?text=Sin+Imagen';
  }

  // Modificamos para aceptar cambios (+1 o -1)
  modifyCart(product: PublicProduct, change: number) {
    if (change > 0) {
      // Intentar agregar
      const success = this.cartService.addToCart(product, 1);
      if (!success) {
        this.messageService.add({
          severity: 'warn',
          summary: 'Stock Limitado',
          detail: `Solo quedan ${product.stockCurrent} unidades.`,
          life: 2000
        });
      } else {
        this.messageService.add({
          severity: 'success',
          summary: 'Agregado',
          detail: `+1 ${product.name}`,
          life: 1000
        });
      }
    } else {
      // Disminuir
      this.cartService.updateQuantity(product.id, -1);
    }
  }

  // Helper para el HTML
  getProductQty(id: string): number {
    return this.cartService.getQuantity(id);
  }
}