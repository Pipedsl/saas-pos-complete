import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { CartItem } from '../../../core/models/cart.model';
import { Product } from '../../../core/models/product.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

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

    // Nuevo: Método para validar stock antes de agregar
    addToCart(product: Product, qtyToAdd: number = 1): boolean {
        const currentItems = this.itemsSubject.value;
        const existingItem = currentItems.find(i => i.product.id === product.id);

        // Cantidad total que tendríamos en el carrito
        const totalQtyInCart = (existingItem ? existingItem.quantity : 0) + qtyToAdd;

        // 1. VALIDACIÓN DE STOCK
        if (totalQtyInCart > product.stockCurrent) {
            return false; // Indica que falló por falta de stock
        }

        const priceWithTax = product.priceNeto * (1 + (product.taxPercent || 19) / 100);

        if (existingItem) {
            existingItem.quantity += qtyToAdd;
            existingItem.subtotal = Math.round(existingItem.quantity * existingItem.unitPrice);
        } else {
            currentItems.push({
                product: product,
                quantity: qtyToAdd,
                unitPrice: Math.round(priceWithTax),
                subtotal: Math.round(priceWithTax * qtyToAdd)
            });
        }

        this.updateState(currentItems);
        return true; // Éxito
    }

    // Nuevo: Disminuir cantidad (sin borrar todo)
    decreaseQuantity(productId: string) {
        const currentItems = this.itemsSubject.value;
        const item = currentItems.find(i => i.product.id === productId);

        if (item) {
            item.quantity -= 1;
            if (item.quantity <= 0) {
                this.removeFromCart(productId);
            } else {
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
        // Crear nueva referencia de array para que Angular detecte el cambio
        this.itemsSubject.next([...items]);
        const total = items.reduce((sum, item) => sum + item.subtotal, 0);
        this.totalSubject.next(total);
    }

    processSale(paymentMethod: string): Observable<any> {
        const payload = {
            items: this.getCurrentItems().map(i => ({
                productId: i.product.id,
                quantity: i.quantity,
                unitPrice: i.unitPrice
            })),
            totalAmount: this.getCurrentTotal()
        };

        return this.http.post(`${environment.apiUrl}/api/sales`, payload);
    }


}