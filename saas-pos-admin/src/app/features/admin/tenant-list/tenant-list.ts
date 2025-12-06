import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { AdminService, TenantSummary } from '../services/admin.service';
import { CommonModule, DatePipe } from '@angular/common';
import { PrimeImportsModule } from '../../../prime-imports';
import { MessageService } from 'primeng/api';
import { AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-tenant-list',
  imports: [CommonModule, PrimeImportsModule, DatePipe, FormsModule],
  templateUrl: './tenant-list.html',
  styleUrl: './tenant-list.css',
})
export class TenantListComponent implements OnInit {

  tenants: TenantSummary[] = [];
  loading = true;
  today = new Date();

  showSubDialog = false;
  selectedTenant: TenantSummary | null = null;

  // Modelo del formulario de renovación
  subForm: any = {
    monthsToAdd: null,
    extraCashiers: 0,
    status: null,
    newPlan: null
  };

  planOptions = [
    { label: 'Solo actualizar datos', value: 0 },
    { label: '+ 1 Mes', value: 1 },
    { label: '+ 3 Meses', value: 3 },
    { label: '+ 6 Meses', value: 6 },
    { label: '+ 1 Año', value: 12 }
  ];
  filteredPlanOptions: any[] = [];

  statusOptions = [
    { label: 'Activo', value: 'ACTIVE' },
    { label: 'Suspendido (No pagó)', value: 'SUSPENDED' }
  ];
  filteredStatusOptions: any[] = [];

  availablePlans = [
    { label: 'Sin Cambios', value: '' },
    { label: 'Plan Básico', value: 'BASIC' },
    { label: 'Plan Pro', value: 'PRO' },
    { label: 'Plan Full', value: 'FULL' }
  ];
  filteredAvailablePlans: any[] = [];

  constructor(
    private adminService: AdminService,
    private cd: ChangeDetectorRef,
    private messageService: MessageService
  ) { }

  ngOnInit(): void {
    this.loadTenants();
  }

  loadTenants() {
    this.loading = true;
    this.adminService.getAllTenants().subscribe({
      next: (data) => {
        this.tenants = data;
        this.loading = false;

        this.cd.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando tenants', err);
        this.loading = false;
        this.cd.detectChanges();
      }
    });
  }

  openSubscription(tenant: TenantSummary) {
    this.selectedTenant = tenant;

    // 1. Definir el estado actual (Esto es lo que faltaba)
    const currentStatus = this.statusOptions.find(s => s.value === (tenant.active ? 'ACTIVE' : 'SUSPENDED'));

    // 2. Inicializar el formulario con los valores por defecto
    this.subForm = {
      monthsToAdd: this.planOptions[0], // Por defecto: "Solo actualizar datos"
      extraCashiers: 0,
      status: currentStatus, // Ahora sí existe la variable
      newPlan: this.availablePlans[0] // Por defecto: "Sin Cambios"
    };

    this.showSubDialog = true;
  }

  filterAvailablePlans(event: AutoCompleteCompleteEvent) {
    const query = event.query.toLowerCase();
    this.filteredAvailablePlans = this.availablePlans.filter(opt => opt.label.toLowerCase().includes(query));
  }

  // FILTROS AUTOCOMPLETE
  filterPlans(event: AutoCompleteCompleteEvent) {
    const query = event.query.toLowerCase();
    this.filteredPlanOptions = this.planOptions.filter(opt => opt.label.toLowerCase().includes(query));
  }

  filterStatus(event: AutoCompleteCompleteEvent) {
    const query = event.query.toLowerCase();
    this.filteredStatusOptions = this.statusOptions.filter(opt => opt.label.toLowerCase().includes(query));
  }

  saveSubscription() {
    if (!this.selectedTenant) return;

    // --- 1. EXTRACCIÓN ROBUSTA DE DATOS ---
    // El AutoComplete a veces guarda el objeto completo y a veces no, esto lo blinda.

    // A. Meses
    let months = 0;
    if (this.subForm.monthsToAdd && typeof this.subForm.monthsToAdd === 'object') {
      months = this.subForm.monthsToAdd.value;
    } else if (typeof this.subForm.monthsToAdd === 'number') {
      months = this.subForm.monthsToAdd;
    }

    // B. Estado
    let statusVal = 'ACTIVE';
    if (this.subForm.status && typeof this.subForm.status === 'object') {
      statusVal = this.subForm.status.value;
    } else if (typeof this.subForm.status === 'string') {
      statusVal = this.subForm.status;
    }

    // C. Plan (Aquí estaba el fallo del payload vacío)
    let planName = '';
    if (this.subForm.newPlan && typeof this.subForm.newPlan === 'object') {
      planName = this.subForm.newPlan.value;
    } else if (typeof this.subForm.newPlan === 'string') {
      planName = this.subForm.newPlan;
    }

    // --- 2. REGLA DE NEGOCIO: PLAN BÁSICO SIN EXTRAS ---
    if (planName === 'BASIC' && this.subForm.extraCashiers > 0) {
      // Mostramos alerta y cancelamos
      alert('Regla de Negocio: El Plan Básico solo permite 1 usuario (Admin). No puedes agregar cajeros extra.');
      return;
    }

    // --- 3. ENVIAR PAYLOAD ---
    this.loading = true;

    const payload = {
      monthsToAdd: months,
      extraCashiers: this.subForm.extraCashiers,
      status: statusVal,
      newPlanName: planName // Ahora sí lleva el valor correcto
    };

    console.log('Enviando renovación:', payload);

    this.adminService.updateSubscription(this.selectedTenant.id, payload).subscribe({
      next: () => {
        // this.messageService.add(...) // Si tienes message service
        alert('Suscripción actualizada correctamente');
        this.showSubDialog = false;
        this.loadTenants();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        alert('Error al actualizar');
      }
    });
  }
  // --- MÉTODO QUE FALTABA PARA EL HTML ---
  getPlanSeverity(plan: string): "success" | "info" | "warn" | "danger" | "secondary" | "contrast" | undefined {
    if (plan === 'DEMO') return 'warn';    // Amarillo
    if (plan === 'PRO') return 'success';  // Verde
    if (plan === 'BASIC') return 'info';   // Azul
    return 'secondary';
  }
}


