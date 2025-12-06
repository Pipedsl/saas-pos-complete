import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
    const router = inject(Router);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {

            // Si el backend responde 401 (No autorizado) o 403 (Prohibido/Token Vencido)
            if (error.status === 401 || error.status === 403) {
                console.warn('Token vencido o inválido. Cerrando sesión...');

                // 1. Borrar basura del storage
                localStorage.clear();

                // 2. Redirigir al login
                router.navigate(['/login']);
            }

            // Propagar el error para que el componente sepa que falló (si es necesario)
            return throwError(() => error);
        })
    );
};