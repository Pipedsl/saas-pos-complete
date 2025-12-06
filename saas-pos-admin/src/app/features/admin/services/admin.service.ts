import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

export interface TenantSummary {
    id: string;
    companyName: string;
    ownerName: string;
    ownerEmail: string;
    planName: string;
    active: boolean;
    createdAt: string;
    demoExpiresAt: string;
    subscriptionEndDate?: string;
}

@Injectable({
    providedIn: 'root'
})
export class AdminService {
    private apiUrl = `${environment.apiUrl}`;

    constructor(private http: HttpClient) { }

    getAllTenants(): Observable<TenantSummary[]> {
        return this.http.get<TenantSummary[]>(`${this.apiUrl}/api/admin/tenants`);
    }

    updateSubscription(tenantId: string, data: any): Observable<void> {
        return this.http.post<void>(`${this.apiUrl}/api/admin/tenant/${tenantId}/subscription`, data);
    }
}