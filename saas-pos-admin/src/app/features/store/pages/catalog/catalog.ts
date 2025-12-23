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
    private cartService: CartService, // Inyectar
    private messageService: MessageService, // Para la notificación
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

  // --- NUEVO MÉTODO ---
  addToCart(product: PublicProduct) {
    this.cartService.addToCart(product);

    // Feedback visual simple
    this.messageService.add({
      severity: 'success',
      summary: 'Agregado',
      detail: `${product.name} al carrito`,
      life: 1500
    });
  }
}
