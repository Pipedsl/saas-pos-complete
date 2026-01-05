import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface InventoryLog {
    id: string;
    productNameSnapshot: string;
    userNameSnapshot: string;
    actionType: string;
    quantityChange: number;
    oldStock: number;
    newStock: number;
    reason: string;
    createdAt: string;
    saleId?: string;
    webOrderId?: string;
}

@Injectable({
    providedIn: 'root'
})
export class ReportsService {
    private apiUrl = `${environment.apiUrl}/api/reports`;

    constructor(private http: HttpClient) { }

    // Obtener datos para la tabla (JSON)
    getInventoryLogs(start?: string, end?: string, categoryId?: string): Observable<InventoryLog[]> {
        let params = new HttpParams();
        if (start) params = params.set('start', start);
        if (end) params = params.set('end', end);
        if (categoryId) params = params.set('categoryId', categoryId);

        return this.http.get<InventoryLog[]>(`${this.apiUrl}/inventory-logs`, { params });
    }

    // Descargar CSV (Archivo Blob)
    downloadCsv(start?: string, end?: string, categoryId?: string) {
        let params = new HttpParams();
        if (start) params = params.set('start', start);
        if (end) params = params.set('end', end);
        if (categoryId) params = params.set('categoryId', categoryId);

        // responseType: 'blob' es vital para descargar archivos binarios/texto como streams
        return this.http.get(`${this.apiUrl}/inventory-logs/csv`, {
            params,
            responseType: 'blob'
        });
    }
}