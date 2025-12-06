import { Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable, tap } from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class OnboardingService {
    private apiUrl = `${environment.apiUrl}/api/public/register`;

    constructor(private http: HttpClient) { }

    register(data: any): Observable<any> {
        return this.http.post<any>(this.apiUrl, data).pipe(
            tap(response => {
                //Si el registro es exitoso guardamos el token y logueamos automaticamente
                if (response.token) {
                    localStorage.setItem('token', response.token);
                    localStorage.setItem('user', JSON.stringify({
                        email: response.email,
                        role: response.role,
                        tenantId: response.tenantId,
                        fullName: response.fullName
                    }));
                }
            })
        );
    }
}