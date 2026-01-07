import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats, ProductRanking } from './services/dashboard.service';
import { PrimeImportsModule } from '../../prime-imports';
import { Product } from '../../core/models/product.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ProductsService } from '../../core/services/products.service';
import { MessageService } from 'primeng/api';
import { ShopConfigService } from '../../core/services/shop-config.service';
import { CategoriesService } from '../../core/services/categories.service';
import { FormsModule } from '@angular/forms';


@Component({
    selector: 'app-dashboard',
    imports: [CommonModule, PrimeImportsModule, FormsModule],
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

    showSalesDetailDialog = false;
    salesDetailData: any[] = [];
    salesDetailTitle = '';
    isLoadingDetails = false;

    showRankingDialog = false;
    rankingData: ProductRanking[] = [];

    // Filtro de fecha actual (YYYY-MM-DD)
    todayDate: string = '';

    // VARIABLES DE FILTRO
    selectedRangeLabel: string = 'Hoy';
    currentStartDate: string = '';
    currentEndDate: string = '';

    ranges = [
        { label: 'Hoy', value: 'TODAY' },
        { label: 'Esta Semana', value: 'WEEK' },
        { label: 'Este Mes', value: 'MONTH' },
        { label: 'Este Año', value: 'YEAR' }
    ];
    selectedRangeValue: string = 'TODAY'; // Para el selector visual

    // VARIABLES GRÁFICO
    chartData: any;
    chartOptions: any;

    categories: any[] = [];
    selectedCategoryId: string = '';

    rankingTypes = [
        { label: 'Más Vendidos', value: 'MOST_SOLD' },
        { label: 'Menos Vendidos', value: 'LEAST_SOLD' },
        { label: 'Sin Ventas (0)', value: 'UNSOLD' }
    ];
    selectedRankingType: string = 'MOST_SOLD';

    constructor(
        private dashboardService: DashboardService,
        private productService: ProductsService,
        private categoriesService: CategoriesService,
        private cd: ChangeDetectorRef,
        private http: HttpClient,

    ) { }

    ngOnInit() {
        this.initChartOptions();
        this.setRange('TODAY');
        this.loadCategories(); // <--- CARGAMOS CATEGORÍAS AL INICIO
    }

    // --- NUEVO: Cargar Categorías para el filtro ---
    loadCategories() {
        this.categoriesService.getCategories().subscribe(cats => {
            this.categories = cats;
        });
    }

    setRange(range: string) {
        this.selectedRangeValue = range;
        const now = new Date();
        const start = new Date();
        const end = new Date(); // Hoy

        if (range === 'TODAY') {
            // Start y End son hoy
            this.selectedRangeLabel = 'Hoy';
        } else if (range === 'WEEK') {
            // Primer día de la semana (Lunes)
            const day = start.getDay() || 7; // Ajuste para que Lunes sea 1
            if (day !== 1) start.setHours(-24 * (day - 1));
            this.selectedRangeLabel = 'Esta Semana';
        } else if (range === 'MONTH') {
            start.setDate(1); // Día 1 del mes
            this.selectedRangeLabel = 'Este Mes';
        } else if (range === 'YEAR') {
            start.setMonth(0, 1); // 1 de Enero
            this.selectedRangeLabel = 'Este Año';
        }

        // Formato YYYY-MM-DD
        this.currentStartDate = start.toISOString().split('T')[0];
        this.currentEndDate = end.toISOString().split('T')[0];

        this.loadStats();
        this.loadChart(range);
        this.cd.detectChanges();
    }

    loadStats() {
        this.dashboardService.getStats(this.currentStartDate, this.currentEndDate).subscribe({
            next: (data) => {
                this.stats = data;
                this.cd.detectChanges();
            },
            error: (err) => console.error('Error dashboard', err)
        });
    }

    loadChart(range: string) {
        if (range === 'TODAY') {
            this.chartData = null;
            return;
        }

        const groupBy = range === 'YEAR' ? 'MONTH' : 'DAY';

        this.dashboardService.getChartData(this.currentStartDate, this.currentEndDate, groupBy).subscribe({
            next: (data) => {
                const labels = data.map(d => d.label);
                const values = data.map(d => d.value);

                this.chartData = {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Ventas ($)',
                            data: values,
                            backgroundColor: 'rgba(59, 130, 246, 0.15)',
                            borderColor: '#60A5FA',
                            borderWidth: 3,
                            pointBackgroundColor: '#60A5FA',
                            pointBorderColor: '#fff',
                            pointHoverBackgroundColor: '#fff',
                            pointHoverBorderColor: '#60A5FA',
                            fill: true,
                            tension: 0.4
                        }
                    ]
                };
                this.cd.detectChanges();
            }
        });
    }

    initChartOptions() {
        const textColor = '#e5e7eb';
        const textColorSecondary = '#9ca3af';
        const surfaceBorder = 'rgba(255, 255, 255, 0.1)';

        this.chartOptions = {
            maintainAspectRatio: false,
            aspectRatio: 0.6,
            plugins: {
                legend: {
                    labels: {
                        color: textColor,
                        font: { weight: 'bold' }
                    }
                },
                tooltip: {
                    backgroundColor: '#1f2937',
                    titleColor: '#fff',
                    bodyColor: '#e5e7eb',
                    borderColor: 'rgba(255,255,255,0.1)',
                    borderWidth: 1,
                    padding: 10,
                    displayColors: false,
                    callbacks: {
                        label: function (context: any) {
                            let label = context.dataset.label || '';
                            if (label) {
                                label += ': ';
                            }
                            if (context.parsed.y !== null) {
                                label += new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP' }).format(context.parsed.y);
                            }
                            return label;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: { color: textColorSecondary },
                    grid: { color: surfaceBorder, drawBorder: false }
                },
                y: {
                    ticks: {
                        color: textColorSecondary,
                        callback: function (value: any) {
                            if (value >= 1000000) return '$' + (value / 1000000).toFixed(1) + 'M';
                            if (value >= 1000) return '$' + (value / 1000).toFixed(0) + 'k';
                            return '$' + value;
                        }
                    },
                    grid: { color: surfaceBorder, drawBorder: false }
                }
            }
        };
    }

    openLowStockDetails() {
        this.productService.getLowStockProducts().subscribe({
            next: (data) => {
                this.lowStockProducts = data;
                this.showLowStockDialog = true;
                this.cd.detectChanges();
            },
            error: (err) => console.error('Error al obtener productos con stock crítico', err)
        });
    }

    openSalesDetail(type: 'SALES' | 'TICKETS') {
        this.salesDetailTitle = type === 'SALES'
            ? `Ventas: ${this.selectedRangeLabel}`
            : `Tickets: ${this.selectedRangeLabel}`;

        this.isLoadingDetails = true;
        this.showSalesDetailDialog = true;

        this.dashboardService.getSalesDetail(this.currentStartDate, this.currentEndDate).subscribe({
            next: (data) => {
                this.salesDetailData = data;
                this.isLoadingDetails = false;
                this.cd.detectChanges();
            },
            error: (err) => { console.error(err); this.isLoadingDetails = false; }
        });
    }

    // --- MODIFICADO: ABRIR RANKING CON RESETEO DE FILTROS ---
    openProfitDetail() {
        this.isLoadingDetails = true;
        this.showRankingDialog = true;

        // Resetear filtros al abrir
        this.selectedRankingType = 'MOST_SOLD';
        this.selectedCategoryId = '';

        this.loadRankingData();
    }

    // --- NUEVO: MÉTODO CENTRALIZADO PARA CARGAR RANKING ---
    loadRankingData() {
        this.isLoadingDetails = true;
        // Si es string vacío, pasamos undefined
        const catParam = this.selectedCategoryId || undefined;

        this.dashboardService.getProductRanking(
            this.currentStartDate,
            this.currentEndDate,
            catParam,
            this.selectedRankingType
        ).subscribe({
            next: (data) => {
                this.rankingData = data;
                this.isLoadingDetails = false;
                this.cd.detectChanges();
            },
            error: (err) => {
                console.error(err);
                this.isLoadingDetails = false;
            }
        });
    }
}