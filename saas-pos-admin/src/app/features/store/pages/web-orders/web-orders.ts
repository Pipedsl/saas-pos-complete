import { ChangeDetectorRef, Component, OnInit } from '@angular/core';

import { CommonModule } from '@angular/common';
import { WebOrder } from '../../../../core/models/web-order.model';
import { WebOrderService } from '../../../../core/services/web-order.service';
import { ShopConfigService } from '../../../../core/services/shop-config.service';
import { MessageService } from 'primeng/api';
import { RouterModule } from '@angular/router';
import { PrimeImportsModule } from '../../../../prime-imports';

@Component({
  selector: 'app-web-orders',
  imports: [CommonModule, RouterModule, PrimeImportsModule],
  templateUrl: './web-orders.html',
  styleUrl: './web-orders.css',
})
export class WebOrdersComponent implements OnInit {

  orders: WebOrder[] = [];
  isLoading: boolean = false;
  shopUrl: string = '';

  constructor(
    private webOrderService: WebOrderService,
    private cd: ChangeDetectorRef,
    private shopConfigService: ShopConfigService,
    private messageService: MessageService,
    private router: RouterModule
  ) { }

  ngOnInit(): void {
    this.loadOrders();
    this.loadShopUrl();
  }

  loadShopUrl() {
    this.shopConfigService.getMyConfig().subscribe({
      next: (config) => {
        const baseUrl = window.location.origin;
        // Construye: http://localhost:4200/store/mi-tienda
        this.shopUrl = `${baseUrl}/store/${config.urlSlug}`;
      },
      error: (err) => console.error('Error cargando config de tienda', err)
    });
  }

  shareOnWhatsApp() {
    if (!this.shopUrl) return;
    const text = `¡Hola! 👋 Visita nuestra tienda online y haz tu pedido aquí:\n${this.shopUrl}`;
    const url = `https://wa.me/?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank');
  }

  copyLink() {
    if (!this.shopUrl) return;
    navigator.clipboard.writeText(this.shopUrl).then(() => {
      this.messageService.add({
        severity: 'success',
        summary: '¡Listo!',
        detail: 'Link copiado al portapapeles'
      });
    });
  }

  loadOrders() {
    this.isLoading = true;
    this.webOrderService.getMyWebOrders().subscribe({
      next: (data) => {
        this.orders = data;
        this.isLoading = false;

        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando pedidos:', err);
        this.isLoading = false;
        this.cd.detectChanges();
      }
    });
  }

  getStatusClass(status: string): string {
    // Usamos colores que contrasten bien en ambos modos
    switch (status) {
      case 'PENDING': return 'bg-yellow-500/20 text-yellow-600 dark:text-yellow-400 border border-yellow-500/30';
      case 'PAID': return 'bg-green-500/20 text-green-600 dark:text-green-400 border border-green-500/30';
      case 'CANCELLED': return 'bg-red-500/20 text-red-600 dark:text-red-400 border border-red-500/30';
      case 'SHIPPED': return 'bg-blue-500/20 text-blue-600 dark:text-blue-400 border border-blue-500/30';
      default: return 'bg-gray-500/20 text-gray-600 dark:text-gray-400';
    }
  }

}
