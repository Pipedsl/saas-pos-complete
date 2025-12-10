/** @type {import('tailwindcss').Config} */
module.exports = {
    // 1. Activar estrategia por clase (selector)
    darkMode: 'selector',

    content: [
        "./src/**/*.{html,ts}",
    ],
    theme: {
        extend: {
            colors: {
                // 2. Usar variables CSS en lugar de colores fijos
                primary: 'var(--primary)',
                secondary: 'var(--secondary)',

                // En modo oscuro será slate-900, en claro será slate-50
                darkbg: 'var(--bg-base)',

                // En modo oscuro será slate-800, en claro será blanco
                cardbg: 'var(--bg-card)',

                // Color de texto base (blanco en dark, negro en light)
                textmain: 'var(--text-main)',
                textsec: 'var(--text-sec)'
            }
        },
    },
    plugins: [],
}