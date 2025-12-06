import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { User } from '../../../core/models/user.model';
import { UsersService } from '../services/users.service';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { PrimeImportsModule } from '../../../prime-imports';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-user-list',
  imports: [CommonModule, ReactiveFormsModule, PrimeImportsModule],
  providers: [MessageService],
  templateUrl: './user-list.html',
  styleUrl: './user-list.css',
})
export class UserListComponent implements OnInit {

  users: User[] = [];
  loading = true;
  showDialog = false;
  userForm: FormGroup;

  // Roles disponibles (Datos maestros)
  roles: any[] = [];

  // Lista filtrada para el AutoComplete
  filteredRoles: any[] = [];

  constructor(
    private usersService: UsersService,
    private fb: FormBuilder,
    private messageService: MessageService,
    private cd: ChangeDetectorRef,
    private authService: AuthService
  ) {
    this.userForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      selectedRole: [null, Validators.required] // <--- Usamos un nombre diferente para evitar confusión
    });
  }

  ngOnInit() {
    this.loadUsers();
    this.setupRoles();
  }

  setupRoles() {
    // Roles base para cualquier tienda
    this.roles = [
      { label: 'Administrador de Tienda', value: 'TENANT_ADMIN' },
      { label: 'Cajero (Punto de Venta)', value: 'CASHIER' }
    ];

    // Si soy EL JEFE, agrego la opción de Partner
    if (this.authService.hasRole(['SUPER_ADMIN'])) {
      this.roles.push({ label: 'Vendedor SaaS (Partner)', value: 'VENDOR' });
    }

    // Actualizar lista filtrada inicial
    this.filteredRoles = [...this.roles];
  }

  loadUsers() {
    this.loading = true;
    this.usersService.getUsers().subscribe({
      next: (data) => {
        console.log('Usuarios recibidos:', data);
        this.users = data;
        this.loading = false;
        this.cd.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  // --- Lógica del AutoComplete ---
  filterRoles(event: AutoCompleteCompleteEvent) {
    const query = event.query.toLowerCase();
    this.filteredRoles = this.roles.filter(role =>
      role.label.toLowerCase().includes(query)
    );
  }

  openNew() {
    this.userForm.reset();
    // Pre-seleccionar Cajero por defecto (opcional)
    // this.userForm.patchValue({ selectedRole: this.roles[1] });
    this.showDialog = true;
  }

  saveUser() {
    if (this.userForm.invalid) return;

    const formValue = this.userForm.value;

    // DEBUG: Ver qué estamos enviando en la consola del navegador
    console.log('Form Value:', formValue);

    // Lógica para extraer el valor del Rol de forma segura
    let roleValue = 'CASHIER';

    if (formValue.selectedRole) {
      // Si seleccionaste de la lista, es un objeto {label, value}
      if (typeof formValue.selectedRole === 'object') {
        roleValue = formValue.selectedRole.value;
      }
      // Si por alguna razón es un string directo (raro pero posible)
      else if (typeof formValue.selectedRole === 'string') {
        roleValue = formValue.selectedRole;
      }
    }

    const userPayload: any = {
      fullName: formValue.fullName,
      email: formValue.email,
      password: formValue.password,
      role: roleValue // Valor limpio (ej: "CASHIER")
    };

    console.log('Payload a enviar:', userPayload);

    this.loading = true; // Buen detalle de UX

    // AQUÍ ESTABA EL ERROR: Reemplazamos los (...) por la lógica real
    this.usersService.createUser(userPayload).subscribe({
      next: (newUser) => {
        this.users.push(newUser); // Agregarlo a la tabla visualmente
        this.showDialog = false;  // Cerrar modal
        this.loading = false;
        this.userForm.reset();    // Limpiar formulario
        this.messageService.add({ severity: 'success', summary: 'Éxito', detail: `Usuario ${newUser.fullName} creado.` });
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        const msg = err.error || 'No se pudo crear el usuario';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
      }
    });
  }

  getRoleSeverity(role: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined {
    switch (role) {
      case 'SUPER_ADMIN': return 'danger';
      case 'TENANT_ADMIN': return 'success';
      case 'CASHIER': return 'info';
      default: return 'warn';
    }
  }
}
