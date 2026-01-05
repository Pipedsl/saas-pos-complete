import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { Product } from '../../../core/models/product.model';

// Actualizamos la interfaz para soportar el precio personalizado
export interface CartItem {
    product: Product;
    quantity: number;
    unitPrice: number; // El precio activo (ya sea el original o el editado)
    subtotal: number;
    customPrice?: number; // Nuevo campo opcional
}

@Injectable({
    providedIn: 'root'
})
export class CartService {
    private itemsSubject = new BehaviorSubject<CartItem[]>([]);
    items$ = this.itemsSubject.asObservable();

    private totalSubject = new BehaviorSubject<number>(0);
    total$ = this.totalSubject.asObservable();

    constructor(private http: HttpClient) { }

    getCurrentItems(): CartItem[] {
        return this.itemsSubject.value;
    }

    getCurrentTotal(): number {
        return this.totalSubject.value;
    }

    // Método original con lógica de stock
    addToCart(product: Product, qtyToAdd: number = 1): boolean {
        const currentItems = this.itemsSubject.value;
        const existingItem = currentItems.find(i => i.product.id === product.id);

        const totalQtyInCart = (existingItem ? existingItem.quantity : 0) + qtyToAdd;

        if (totalQtyInCart > product.stockCurrent) {
            return false;
        }

        // Calcular precio base (Tu lógica original)
        let finalUnitPrice: number;
        if (product.priceFinal && product.priceFinal > 0) {
            finalUnitPrice = product.priceFinal;
        } else {
            const rawPrice = product.priceNeto * (1 + (product.taxPercent || 19) / 100);
            finalUnitPrice = Math.round(rawPrice);
        }

        if (existingItem) {
            existingItem.quantity += qtyToAdd;
            // OJO: Si ya tenía un customPrice, lo respetamos. Si no, usamos el calculado.
            const activePrice = existingItem.customPrice !== undefined ? existingItem.customPrice : finalUnitPrice;

            existingItem.unitPrice = activePrice;
            existingItem.subtotal = existingItem.quantity * activePrice;
        } else {
            currentItems.push({
                product: product,
                quantity: qtyToAdd,
                unitPrice: finalUnitPrice,
                subtotal: qtyToAdd * finalUnitPrice
                // customPrice empieza undefined
            });
        }

        this.updateState(currentItems);
        return true;
    }

    // --- NUEVO MÉTODO: EDITAR PRECIO (Lógica del PIN) ---
    updateItemPrice(productId: string, newPrice: number) {
        const currentItems = this.itemsSubject.value;
        const item = currentItems.find(i => i.product.id === productId);

        if (item) {
            item.customPrice = newPrice; // Guardamos que es personalizado
            item.unitPrice = newPrice;   // Actualizamos el precio activo
            item.subtotal = item.quantity * newPrice; // Recalcular subtotal
            this.updateState([...currentItems]);
        }
    }

    decreaseQuantity(productId: string) {
        const currentItems = this.itemsSubject.value;
        const item = currentItems.find(i => i.product.id === productId);

        if (item) {
            item.quantity -= 1;
            if (item.quantity <= 0) {
                this.removeFromCart(productId);
            } else {
                // Usamos item.unitPrice que ya tiene el precio correcto (custom o base)
                item.subtotal = Math.round(item.quantity * item.unitPrice);
                this.updateState(currentItems);
            }
        }
    }

    removeFromCart(productId: string) {
        const currentItems = this.itemsSubject.value.filter(i => i.product.id !== productId);
        this.updateState(currentItems);
    }

    clearCart() {
        this.updateState([]);
    }

    private updateState(items: CartItem[]) {
        this.itemsSubject.next([...items]);
        const total = items.reduce((sum, item) => sum + item.subtotal, 0);
        this.totalSubject.next(total);
    }

    processSale(paymentMethod: string): Observable<any> {
        const payload = {
            items: this.getCurrentItems().map(i => ({
                productId: i.product.id,
                quantity: i.quantity,
                unitPrice: i.unitPrice, // Precio visual (para referencia)
                customPrice: i.customPrice // IMPORTANTE: Enviar esto al backend para la auditoría
            })),
            totalAmount: this.getCurrentTotal(),
            notes: `Venta POS - Pago: ${paymentMethod}`
        };

        return this.http.post(`${environment.apiUrl}/api/sales`, payload);
    }
}