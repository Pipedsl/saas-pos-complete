import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login';
import { MainLayoutComponent } from './shared/components/main-layout/main-layout';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { RegisterComponent } from './features/auth/register/register';


export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },
    {
        path: '',
        component: MainLayoutComponent,
        canActivate: [authGuard],
        children: [
            { path: 'admin/tenants', loadComponent: () => import('./features/admin/tenant-list/tenant-list').then(m => m.TenantListComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN'] } },
            { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN', 'VENDOR'] } },
            { path: 'products', loadComponent: () => import('./features/products/product-list/product-list').then(m => m.ProductListComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] } },
            { path: 'products/new', loadComponent: () => import('./features/products/product-form/product-form').then(m => m.ProductFormComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] } },
            {
                path: 'products/edit/:id', // <--- NUEVA RUTA
                loadComponent: () => import('./features/products/product-form/product-form').then(m => m.ProductFormComponent),
                canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] }
            },
            { path: 'vendors', loadComponent: () => import('./features/users/user-list/user-list').then(m => m.UserListComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] } },
            { path: 'settings', loadComponent: () => import('./features/settings/settings').then(m => m.SettingsComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] } },
            { path: 'pos', loadComponent: () => import('./features/sales/pos/pos').then(m => m.PosComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN', 'CASHIER'] } },
            { path: 'saas/links', loadComponent: () => import('./features/users/links-manager/links-manager').then(m => m.LinksManagerComponent), canActivate: [roleGuard], data: { roles: ['SUPER_ADMIN', 'VENDOR'] } },
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
        ]
    },
    { path: '**', redirectTo: 'dashboard' } // Cualquier ruta rota al login
];