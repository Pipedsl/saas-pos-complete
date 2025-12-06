import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

export interface DemoLink {
    token: string;
    url: string;
    expiresAt: string;
    used: boolean;
    createdAt: string;
}

@Injectable({
    providedIn: 'root'
})
export class SalesAgentService {
    private apiUrl = `${environment.apiUrl}`;

    constructor(private http: HttpClient) { }

    generateLink(): Observable<DemoLink> {
        return this.http.post<DemoLink>(`${this.apiUrl}/api/agent/generate-link`, {});
    }

    //TODO: falta un endpoint en el backend para listar los links generados
}