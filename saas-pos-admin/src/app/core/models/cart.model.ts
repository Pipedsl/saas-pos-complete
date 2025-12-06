import { Product } from '../models/product.model';

export interface CartItem {
    product: Product;
    quantity: number; // Puede ser decimal (0.5 kg)
    unitPrice: number; // Precio al momento de la venta
    subtotal: number; // quantity * unitPrice
}

export interface SaleRequest {
    items: { productId: string, quantity: number }[];
    paymentMethod: 'CASH' | 'CARD' | 'TRANSFER';
    totalAmount: number;
}