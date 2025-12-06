export interface Product {
    id: string;
    sku: string;
    name: string;
    description?: string;
    priceNeto: number;
    measurementUnit: 'UNIT' | 'KG';
    taxPercent: number;
    stockCurrent: number;
    stockMin: number;
    //El JSONB se mapea asi en el front:
    attributes?: { [key: string]: any };
    categoryId?: string;
    categoryName?: string;
    costPrice: number;      // Costo Neto Compra
    marginPercent?: number; // Margen deseado
    finalPrice?: number;    // Precio Venta Final (Calculado)
    supplierId?: string;
    supplierName?: string;
}