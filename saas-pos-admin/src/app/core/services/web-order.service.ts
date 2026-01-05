import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WebOrder } from '../models/web-order.model';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class WebOrderService {
    // Ajusta la URL base si tu environment.apiUrl no incluye '/api'
    private apiUrl = `${environment.apiUrl}/api/web-orders`;

    constructor(private http: HttpClient) { }

    getMyWebOrders(): Observable<WebOrder[]> {
        return this.http.get<WebOrder[]>(this.apiUrl);
    }
}