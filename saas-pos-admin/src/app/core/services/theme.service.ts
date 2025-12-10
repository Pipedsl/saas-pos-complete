import { Injectable, signal } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class ThemeService {

    // Usamos signals para reactividad moderna (opcional, pero recomendado en Angular 17+)
    isDark = signal<boolean>(true); // Empezamos en Dark por defecto

    constructor() {
        // Leer preferencia guardada
        const savedTheme = localStorage.getItem('theme');

        if (savedTheme === 'light') {
            this.setLightMode();
        } else {
            this.setDarkMode(); // Por defecto Dark
        }
    }

    toggleTheme() {
        if (this.isDark()) {
            this.setLightMode();
        } else {
            this.setDarkMode();
        }
    }

    private setDarkMode() {
        document.documentElement.classList.add('dark');
        localStorage.setItem('theme', 'dark');
        this.isDark.set(true);
    }

    private setLightMode() {
        document.documentElement.classList.remove('dark');
        localStorage.setItem('theme', 'light');
        this.isDark.set(false);
    }
}