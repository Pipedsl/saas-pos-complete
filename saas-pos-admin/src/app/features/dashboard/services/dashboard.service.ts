import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface DashboardStats {
    totalSalesToday: number;
    totalTransactionsToday: number;
    lowStockCount: number;
    totalProfitToday: number;
}

export interface ProductRanking {
    productName: string;
    sku: string;
    categoryName: string;
    totalQuantitySold: number;
    totalRevenue: number;
}

@Injectable({
    providedIn: 'root'
})
export class DashboardService {
    private apiUrl = `${environment.apiUrl}/api/dashboard`;

    constructor(private http: HttpClient) { }

    getStats(startDate?: string, endDate?: string): Observable<DashboardStats> {
        let url = `${this.apiUrl}/stats`;
        if (startDate && endDate) {
            url += `?startDate=${startDate}&endDate=${endDate}`;
        }
        return this.http.get<DashboardStats>(url);
    }

    // 1. Obtener detalle de ventas (Lista de Sales completa)
    getSalesDetail(startDate: string, endDate: string): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/sales-detail?startDate=${startDate}&endDate=${endDate}`);
    }

    // 2. Obtener Ranking de Productos
    getProductRanking(startDate: string, endDate: string, categoryId?: string, type: string = 'MOST_SOLD'): Observable<ProductRanking[]> {
        let url = `${this.apiUrl}/product-ranking?startDate=${startDate}&endDate=${endDate}&type=${type}`;
        if (categoryId) {
            url += `&categoryId=${categoryId}`;
        }
        return this.http.get<ProductRanking[]>(url);
    }

    getChartData(startDate: string, endDate: string, groupBy: 'DAY' | 'MONTH'): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/chart-data?startDate=${startDate}&endDate=${endDate}&groupBy=${groupBy}`);
    }
}