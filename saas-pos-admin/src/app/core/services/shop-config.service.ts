import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ShopConfig {
    id?: string;
    tenantId?: string;
    urlSlug: string;
    shopName: string;

    // Branding
    logoUrl?: string;
    bannerUrl?: string;
    primaryColor?: string;
    contactPhone?: string;
    reservationMinutes?: number;

    // Configuración de métodos
    paymentMethods: {
        cash: boolean;
        transfer: boolean;
        [key: string]: boolean;
    };

    shippingMethods: {
        pickup: boolean;
        delivery: boolean;
        companies?: string[]; // Array de strings (códigos de couriers)
        [key: string]: any;
    };

    active: boolean;

    recommendedCourier?: string;
    dispatchDays?: { [key: string]: string[] };
}

@Injectable({
    providedIn: 'root'
})
export class ShopConfigService {
    private apiUrl = `${environment.apiUrl}/api/admin/shop-config`;

    constructor(private http: HttpClient) { }

    getMyConfig(): Observable<ShopConfig> {
        return this.http.get<ShopConfig>(this.apiUrl);
    }

    updateConfig(config: ShopConfig): Observable<ShopConfig> {
        return this.http.put<ShopConfig>(this.apiUrl, config);
    }
}