import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

// Reutilizamos modelos o definimos interfaces simples aquí
export interface SaleItem {
    id?: string;
    productId?: string; // Para el request
    product?: any;      // Para la respuesta (objeto completo)
    quantity: number;
    unitPrice: number;
    total: number;
}

export interface Sale {
    id: string;
    saleNumber?: string;
    totalAmount: number;
    status: string;
    createdAt: string;
    items: SaleItem[];
    editReason?: string;
    wasEdited?: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class SalesService {
    private apiUrl = `${environment.apiUrl}/api/sales`;

    constructor(private http: HttpClient) { }

    getSales(): Observable<Sale[]> {
        return this.http.get<Sale[]>(this.apiUrl);
    }

    getSaleById(id: string): Observable<Sale> {
        return this.http.get<Sale>(`${this.apiUrl}/${id}`);
    }

    // Editar Venta (Devolución / Corrección)
    updateSale(saleId: string, items: any[], notes: string): Observable<Sale> {
        const payload = {
            items: items.map(item => ({
                productId: item.product?.id || item.productId, // Manejar ambos casos
                quantity: item.quantity,
                unitPrice: item.unitPrice
            })),
            notes: notes
        };
        return this.http.put<Sale>(`${this.apiUrl}/${saleId}`, payload);
    }
}