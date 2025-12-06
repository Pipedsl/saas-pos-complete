import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export interface AuthResponse {
    token: string;
    email: string;
    role: string;
    tenantId: string;
    fullName: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private apiUrl = `${environment.apiUrl}`;

    constructor(private http: HttpClient) { }

    private isTokenExpired(token: string): boolean {
        try {
            const expiry = (JSON.parse(atob(token.split('.')[1]))).exp;
            return (Math.floor((new Date).getTime() / 1000)) >= expiry;
        } catch (e) {
            return true; // Si no se puede leer, asumimos vencido
        }
    }

    login(credentials: { email: string, password: string }): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, credentials).pipe(
            tap(response => {
                //Guardamos el token en el localStorage
                localStorage.setItem('token', response.token);
                localStorage.setItem('user', JSON.stringify({
                    email: response.email,
                    role: response.role,
                    tenantId: response.tenantId,
                    fullName: response.fullName
                }));
            }
            ));
    }

    logout() {
        localStorage.clear();
        //TODO: Redirigir al login si es necesario
    }

    isAuthenticated(): boolean {
        const token = localStorage.getItem('token');

        if (!token) return false;

        // NUEVO: Si existe pero está vencido, lo borramos y retornamos false
        if (this.isTokenExpired(token)) {
            this.logout();
            return false;
        }

        return true;
    }

    getCurrentUser() {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    }

    // Obtener solo el rol
    getRole(): string {
        const user = this.getCurrentUser();
        return user ? user.role : '';
    }

    // Método Maestro: ¿Tiene el usuario uno de estos roles permitidos?
    hasRole(allowedRoles: string[]): boolean {
        const myRole = this.getRole();
        // Si es SUPER_ADMIN, tiene permiso para todo (God Mode)
        if (myRole === 'SUPER_ADMIN') return true;

        return allowedRoles.includes(myRole);
    }

    refreshUser(): Observable<User> {
        return this.http.get<User>(`${this.apiUrl}/api/users/me`).pipe(
            tap(user => {
                // Actualizar localStorage con datos frescos
                const currentData = this.getCurrentUser();
                const newData = { ...currentData, fullName: user.fullName, role: user.role, email: user.email };
                localStorage.setItem('user', JSON.stringify(newData));
            })
        );
    }
}