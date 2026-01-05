import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from './services/dashboard.service';
import { PrimeImportsModule } from '../../prime-imports';
import { Product } from '../../core/models/product.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ProductsService } from '../../core/services/products.service';
import { MessageService } from 'primeng/api';
import { ShopConfigService } from '../../core/services/shop-config.service';

@Component({
    selector: 'app-dashboard',
    imports: [CommonModule, PrimeImportsModule],
    providers: [MessageService],
    templateUrl: './dashboard.html',
    styleUrl: './dashboard.css',
})

export class DashboardComponent implements OnInit {
    stats: DashboardStats = {
        totalSalesToday: 0,
        totalTransactionsToday: 0,
        lowStockCount: 0,
        totalProfitToday: 0
    };

    showLowStockDialog = false;
    lowStockProducts: Product[] = [];


    constructor(
        private dashboardService: DashboardService,
        private productService: ProductsService,
        private cd: ChangeDetectorRef,
        private http: HttpClient,

    ) { }

    ngOnInit() {
        this.loadStats();

    }







    loadStats() {
        this.dashboardService.getStats().subscribe({
            next: (data) => {
                console.log('Datos Dashboard recibidos:', data); // Para ver en consola
                this.stats = data;

                // 3. OBLIGAR A ANGULAR A PINTAR LOS NÚMEROS
                this.cd.detectChanges();
            },
            error: (err) => console.error('Error dashboard', err)
        });
    }

    openLowStockDetails() {

        this.productService.getLowStockProducts().subscribe({
            next: (data) => {
                console.log('Stock Crítico recibido:', data);

                this.lowStockProducts = data;
                this.showLowStockDialog = true;
                this.cd.detectChanges();
            },
            error: (err) => console.error('Error al obtener productos con stock crítico', err)
        })
    }
}