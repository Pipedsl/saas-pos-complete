import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Observable } from 'rxjs';

// 1. Corregir Interfaz para coincidir con el JSON del Backend
export interface WebOrderItem {
    id: string;
    productNameSnapshot: string; // Antes: productName
    skuSnapshot: string;         // Antes: sku
    quantity: number;
    unitPriceAtMoment: number;   // Antes: unitPrice
    subtotal: number;            // Antes: total
    product?: any;  // El objeto producto completo que viene del backend
    maxStock?: number; // Propiedad temporal para el frontend
    productId?: string;
}

export interface WebOrder {
    id: string;
    orderNumber: string;
    customerName: string;
    customerPhone: string;
    customerEmail?: string;
    customerRut?: string;        // Nuevo campo RUT

    shippingRegion?: string;     // Backend envía esto
    shippingCommune?: string;    // Backend envía esto
    shippingStreet?: string;     // Backend envía esto

    shippingAddress?: string;    // Backend envía esto
    shippingNotes?: string;

    courier?: string;
    shippingMethod: 'PICKUP' | 'DELIVERY'; // Backend envía shippingMethod
    paymentMethod: string;

    status: string;
    finalTotal: number;

    items: WebOrderItem[];
    createdAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class AdminOrdersService {
    private apiUrl = `${environment.apiUrl}/api/admin/web-orders`;

    constructor(private http: HttpClient) { }

    getOrderByNumber(orderNumber: string): Observable<WebOrder> {
        return this.http.get<WebOrder>(`${this.apiUrl}/${orderNumber}`);
    }

    // 2. Corregir envío de estado: Enviar texto plano si el backend espera String
    updateStatus(orderNumber: string, status: string): Observable<WebOrder> {
        return this.http.patch<WebOrder>(
            `${this.apiUrl}/${orderNumber}/status`,
            status // Enviamos el string "PAID", no el objeto
        );
    }
    updateOrderItems(orderNumber: string, items: { productId: string, quantity: number }[]): Observable<WebOrder> {
        return this.http.put<WebOrder>(`${this.apiUrl}/${orderNumber}/items`, { items });
    }
}