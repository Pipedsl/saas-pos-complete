export interface User {
    id: string;
    fullName: string;
    email: string;
    role: 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'CASHIER' | 'VENDOR';
    active: boolean;
    createdAt?: string;
    password?: string;
}