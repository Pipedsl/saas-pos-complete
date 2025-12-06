import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { MessageService } from 'primeng/api'; // Opcional, para mostrar alerta

export const roleGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    // Obtenemos los roles permitidos desde la configuración de la ruta
    const expectedRoles = route.data['roles'] as string[];

    if (authService.hasRole(expectedRoles)) {
        return true; // Pase, jefe
    } else {
        // Si es un cajero intentando entrar al dashboard, lo mandamos al POS
        if (authService.getRole() === 'CASHIER') {
            router.navigate(['/pos']);
        } else {
            router.navigate(['/login']);
        }
        return false; // Acceso Denegado
    }
};