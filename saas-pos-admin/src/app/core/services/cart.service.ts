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
  // Fuentes de verdad (Observables)
  private itemsSubject = new BehaviorSubject<CartItem[]>([]);
  items$ = this.itemsSubject.asObservable();

  private countSubject = new BehaviorSubject<number>(0);
  count$ = this.countSubject.asObservable();

  private totalSubject = new BehaviorSubject<number>(0);
  total$ = this.totalSubject.asObservable();

  constructor() {
    // Opcional: Aquí podríamos recuperar del localStorage si quisieras persistencia
  }

  addToCart(product: PublicProduct) {
    const currentItems = this.itemsSubject.value;
    const existingItem = currentItems.find(item => item.product.id === product.id);

    if (existingItem) {
      // Si ya existe, sumamos 1
      existingItem.quantity += 1;
      existingItem.subtotal = existingItem.quantity * existingItem.product.price;
    } else {
      // Si es nuevo, lo agregamos
      currentItems.push({
        product: product,
        quantity: 1,
        subtotal: product.price
      });
    }

    this.updateState([...currentItems]); // Spread operator para gatillar cambios
  }

  removeFromCart(productId: string) {
    const filteredItems = this.itemsSubject.value.filter(item => item.product.id !== productId);
    this.updateState(filteredItems);
  }

  // Aumentar o disminuir desde el checkout
  updateQuantity(productId: string, change: number) {
    const currentItems = this.itemsSubject.value;
    const item = currentItems.find(i => i.product.id === productId);

    if (item) {
      item.quantity += change;
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

  private updateState(items: CartItem[]) {
    this.itemsSubject.next(items);

    // Recalcular total de ítems (ej: 2 cocas + 1 papas = 3 items)
    const count = items.reduce((acc, item) => acc + item.quantity, 0);
    this.countSubject.next(count);

    // Recalcular dinero total
    const total = items.reduce((acc, item) => acc + item.subtotal, 0);
    this.totalSubject.next(total);
  }
}