import { HttpInterceptorFn } from "@angular/common/http";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    // Recuperar el token del almacenamiento
    const token = localStorage.getItem('token');

    //Si existe, clonamos la peticion y le inyectamos el header
    if (token) {
        const cloneReq = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });
        return next(cloneReq);
    }

    //Si no hay token, dejamos pasar la peticion tal cual (ej: login)
    return next(req);
}