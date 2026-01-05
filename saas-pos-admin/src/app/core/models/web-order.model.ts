export interface WebOrder {
    id: string;
    orderNumber: string;
    customerName: string;
    customerPhone: string;
    totalItems: number;
    finalTotal: number;
    status: string; // 'PENDING', 'PAID', etc.
    paymentMethod: string;
    shippingMethod: string;
    createdAt: string;
}