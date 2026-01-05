import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { PublicProduct } from './store.service';

export interface CartItem {
  product: PublicProduct;
  quantity: number;
  subtotal: number;

}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  // Fuentes de verdad
  private itemsSubject = new BehaviorSubject<CartItem[]>([]);
  items$ = this.itemsSubject.asObservable();

  private countSubject = new BehaviorSubject<number>(0);
  count$ = this.countSubject.asObservable();

  private totalSubject = new BehaviorSubject<number>(0);
  total$ = this.totalSubject.asObservable();

  constructor() { }

  // --- MÉTODO CLAVE PARA VALIDAR STOCK ---
  // Retorna TRUE si se pudo agregar, FALSE si no hay stock
  addToCart(product: PublicProduct, quantityToAdd: number = 1): boolean {
    const currentItems = this.itemsSubject.value;
    const existingItem = currentItems.find(item => item.product.id === product.id);

    // Cantidad actual en carrito (0 si no existe)
    const currentQty = existingItem ? existingItem.quantity : 0;

    // Validación: ¿Lo que tengo + lo que quiero agregar supera el stock real?
    if (currentQty + quantityToAdd > product.stockCurrent) {
      return false; // Bloqueamos la acción
    }

    if (existingItem) {
      existingItem.quantity += quantityToAdd;
      existingItem.subtotal = existingItem.quantity * existingItem.product.price;
    } else {
      currentItems.push({
        product: product,
        quantity: quantityToAdd,
        subtotal: product.price * quantityToAdd
      });
    }

    this.updateState([...currentItems]);
    return true; // Éxito
  }

  removeFromCart(productId: string) {
    const filteredItems = this.itemsSubject.value.filter(item => item.product.id !== productId);
    this.updateState(filteredItems);
  }

  updateQuantity(productId: string, change: number) {
    const currentItems = this.itemsSubject.value;
    const item = currentItems.find(i => i.product.id === productId);

    if (item) {
      const newQty = item.quantity + change;

      // Validación de stock al aumentar desde el checkout
      if (change > 0 && newQty > item.product.stockCurrent) {
        return; // No permitimos subir más allá del stock
      }

      item.quantity = newQty;
      if (item.quantity <= 0) {
        this.removeFromCart(productId);
        return;
      }
      item.subtotal = item.quantity * item.product.price;
      this.updateState([...currentItems]);
    }
  }

  clearCart() {
    this.updateState([]);
  }

  getCurrentItems(): CartItem[] {
    return this.itemsSubject.value;
  }

  // --- MÉTODO AUXILIAR PARA LA VISTA ---
  getQuantity(productId: string): number {
    const item = this.itemsSubject.value.find(i => i.product.id === productId);
    return item ? item.quantity : 0;
  }

  private updateState(items: CartItem[]) {
    this.itemsSubject.next(items);
    const count = items.reduce((acc, item) => acc + item.quantity, 0);
    this.countSubject.next(count);
    const total = items.reduce((acc, item) => acc + item.subtotal, 0);
    this.totalSubject.next(total);
  }
}