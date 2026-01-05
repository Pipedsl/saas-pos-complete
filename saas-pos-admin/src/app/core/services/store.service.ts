import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PublicShop {
  shopName: string;
  contactPhone?: string;
  logoUrl: string;
  bannerUrl: string;
  primaryColor: string;
  urlSlug: string;
  paymentMethods: {
    cash: boolean;
    transfer: boolean;
    cod_shipping: boolean; // <--- Nuevo campo
    [key: string]: any;
  };
  shippingMethods: {
    pickup: boolean;
    delivery: boolean;
    companies: string[]; // <--- AQUÍ ESTÁ LA LISTA DE CÓDIGOS (['starken', 'varmontt'])
    [key: string]: any;
  };

  // --- NUEVOS CAMPOS ---
  recommendedCourier?: string;
  dispatchDays?: { [key: string]: string[] };
}

export interface PublicProduct {
  id: string;
  sku: string;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  categoryName: string;
  stockCurrent: number;
  lowStock: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class StoreService {
  private apiUrl = `${environment.apiUrl}/api/public/store`;

  constructor(private http: HttpClient) { }

  // 1. Obtener Info de Tienda (Logo, Colores)
  getShopInfo(slug: string): Observable<PublicShop> {
    return this.http.get<PublicShop>(`${this.apiUrl}/${slug}`);
  }

  // 2. Obtener Catálogo
  getProducts(slug: string): Observable<PublicProduct[]> {
    return this.http.get<PublicProduct[]>(`${this.apiUrl}/${slug}/products`);
  }

  createOrder(slug: string, orderData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/${slug}/orders`, orderData);
  }

}
