export interface Product {
    id: string;
    sku: string;
    name: string;
    description?: string;

    priceNeto: number;      // El valor base (calculado)
    costPrice: number;      // Costo de compra
    taxPercent?: number;    // IVA (ej: 19)

    // NUEVOS CAMPOS (Para la lógica inversa)
    priceFinal?: number;    // El precio bruto que ve el cliente
    isTaxIncluded?: boolean;// Checkbox
    marginPercent?: number; // Calculado para visualización


    stockCurrent: number;
    stockMin: number;
    measurementUnit: 'UNIT' | 'KG';
    attributes?: { [key: string]: any };

    categoryId?: string;
    categoryName?: string;
    supplierId?: string;
    supplierName?: string;

    isActive: boolean;

}