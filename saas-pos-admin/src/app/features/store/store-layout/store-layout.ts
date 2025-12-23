import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs'; // <--- Importante
import { PublicShop, StoreService } from '../../../core/services/store.service';
import { CartService } from '../../../core/services/cart.service'; // <--- RUTA CORREGIDA (apunta a core)

@Component({
  selector: 'app-store-layout',
  imports: [CommonModule, RouterModule],
  templateUrl: './store-layout.html', // Asegúrate que el nombre del archivo HTML coincida
  styleUrls: ['./store-layout.css'] // Asegúrate que el nombre coincida (styleUrls en plural)
})
export class StoreLayoutComponent implements OnInit {
  shopConfig: PublicShop | null = null;
  slug: string = '';
  currentYear = new Date().getFullYear();

  // Declaramos el observable pero NO lo asignamos todavía
  cartCount$!: Observable<number>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private storeService: StoreService,
    private cartService: CartService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    // ASIGNACIÓN AQUÍ: Ahora 'this.cartService' ya está listo
    this.cartCount$ = this.cartService.count$;

    this.route.paramMap.subscribe(params => {
      this.slug = params.get('slug') || '';
      if (this.slug) {
        this.loadShopInfo();
      }
    });
  }

  loadShopInfo() {
    this.storeService.getShopInfo(this.slug).subscribe({
      next: (data) => {
        this.shopConfig = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Tienda no encontrada', err)
    });
  }

  goToCheckout() {
    if (this.slug) {
      this.router.navigate(['/store', this.slug, 'checkout']);
    }
  }
}