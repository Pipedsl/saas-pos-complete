import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PrimeImportsModule } from '../../prime-imports';
import { MessageService } from 'primeng/api';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ShopConfigService, ShopConfig } from '../../core/services/shop-config.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PrimeImportsModule],
  providers: [MessageService],
  templateUrl: './settings.html',
})
export class SettingsComponent implements OnInit {
  // 1. Configuración Existente (POS / Interna)
  settings: any = {
    admin_pin: '',
    shopName: '',
    shopRut: '',
    shopAddress: '',
    footerMessage: '',
    printerType: '58mm',
    ticketPrefix: '',
    bankName: '',
    accountType: '',
    accountNumber: '',
    rut: '',
    email: ''
  };

  // 2. Nueva Configuración (Ecommerce / Tienda Online)
  shopForm: FormGroup;
  isShopLoading = false;

  transportCompanies = [
    { name: 'Starken', code: 'starken' },
    { name: 'Chilexpress', code: 'chilexpress' },
    { name: 'Varmontt', code: 'varmontt' },
    { name: 'Blue Express', code: 'blue_express' },
    { name: 'Correos Chile', code: 'correos' }
  ];

  weekDays = [
    { name: 'Lunes', code: 'Lunes' },
    { name: 'Martes', code: 'Martes' },
    { name: 'Miércoles', code: 'Miércoles' },
    { name: 'Jueves', code: 'Jueves' },
    { name: 'Viernes', code: 'Viernes' },
    { name: 'Sábado', code: 'Sábado' },
    { name: 'Domingo', code: 'Domingo' }
  ];

  constructor(
    private http: HttpClient,
    private messageService: MessageService,
    private fb: FormBuilder,
    private shopConfigService: ShopConfigService
  ) {
    // Inicializamos el formulario de la tienda
    this.shopForm = this.fb.group({
      shopName: ['', [Validators.required, Validators.minLength(3)]],
      urlSlug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      contactPhone: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]],
      primaryColor: ['#3B82F6'],
      reservationMinutes: [60, [Validators.required, Validators.min(1)]],
      logoUrl: [''],
      bannerUrl: [''],
      active: [true],
      paymentMethods: this.fb.group({
        cash: [false],
        transfer: [false],
        cod_shipping: [false]
      }),
      shippingMethods: this.fb.group({
        pickup: [false],
        delivery: [false],
        companies: [[]],
        // Agregamos estos controles al formulario para que guarden el estado visual
        recommendedCourier: [''],
        dispatchDays: [{}]
      })
    });
  }

  ngOnInit() {
    this.loadPosSettings();
    this.loadShopConfig();
  }

  // --- LÓGICA POS (Existente) ---
  loadPosSettings() {
    this.http.get(`${environment.apiUrl}/api/settings`).subscribe({
      next: (data: any) => {
        this.settings = { ...this.settings, ...(data || {}) };
        if (!this.settings.printerType) {
          this.settings.printerType = '58mm';
        }
      },
      error: (err) => console.error('Error cargando configuración POS', err)
    });
  }

  savePosSettings() {
    this.http.post(`${environment.apiUrl}/api/settings`, this.settings).subscribe({
      next: () => this.showSuccess('Configuración POS actualizada'),
      error: (err) => this.showError('No se pudo guardar la configuración POS')
    });
  }

  // --- LÓGICA ECOMMERCE (Nueva) ---
  loadShopConfig() {
    this.isShopLoading = true;
    this.shopConfigService.getMyConfig().subscribe({
      next: (config) => {
        this.shopForm.patchValue({
          shopName: config.shopName,
          urlSlug: config.urlSlug,
          contactPhone: config.contactPhone,
          primaryColor: config.primaryColor || '#3B82F6',
          reservationMinutes: config.reservationMinutes || 60,
          logoUrl: config.logoUrl,
          bannerUrl: config.bannerUrl,
          active: config.active,
          paymentMethods: {
            cash: config.paymentMethods?.['cash'] || false,
            transfer: config.paymentMethods?.['transfer'] || false,
            cod_shipping: config.paymentMethods?.['cod_shipping'] || false
          },
          shippingMethods: {
            pickup: config.shippingMethods?.['pickup'] || false,
            delivery: config.shippingMethods?.['delivery'] || false,
            companies: config.shippingMethods?.['companies'] || [],
            // Mapeamos los datos que vienen del backend (raíz) a los controles del formulario (anidados)
            recommendedCourier: config.recommendedCourier || '',
            dispatchDays: config.dispatchDays || {}
          }
        });

        // Asegurar inicialización de objeto dispatchDays
        const currentShipping = this.shopForm.get('shippingMethods')?.value;
        if (!currentShipping.dispatchDays) {
          currentShipping.dispatchDays = {};
          this.shopForm.get('shippingMethods')?.patchValue(currentShipping);
        }

        this.isShopLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isShopLoading = false;
      }
    });
  }

  updateDispatchDays(courier: string, days: string[]) {
    const shippingControl = this.shopForm.get('shippingMethods');
    const currentVal = shippingControl?.value;

    if (!currentVal.dispatchDays) currentVal.dispatchDays = {};
    currentVal.dispatchDays[courier] = days;

    shippingControl?.patchValue(currentVal);
  }

  onFileSelect(event: any, field: 'logoUrl' | 'bannerUrl') {
    const file = event.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.shopForm.patchValue({ [field]: e.target.result });
        this.messageService.add({ severity: 'info', summary: 'Imagen cargada', detail: 'Recuerda guardar los cambios.' });
      };
      reader.readAsDataURL(file);
    }
  }

  saveShopConfig() {
    if (this.shopForm.invalid) {
      this.shopForm.markAllAsTouched();
      this.messageService.add({ severity: 'warn', summary: 'Formulario Inválido', detail: 'Revisa los campos en rojo' });
      return;
    };

    this.isShopLoading = true;

    // --- CORRECCIÓN AQUÍ ---
    const rawValue = this.shopForm.getRawValue(); // Declaramos la variable

    const configToSave: ShopConfig = {
      ...rawValue,
      // Extraemos los datos anidados a la raíz
      recommendedCourier: rawValue.shippingMethods.recommendedCourier,
      dispatchDays: rawValue.shippingMethods.dispatchDays
    };

    this.shopConfigService.updateConfig(configToSave).subscribe({
      next: (saved) => {
        this.showSuccess('Tienda Online actualizada');
        this.isShopLoading = false;
      },
      error: (err) => {
        this.showError('Error al guardar Tienda Online');
        this.isShopLoading = false;
      }
    });
  }

  private showSuccess(msg: string) {
    this.messageService.add({ severity: 'success', summary: 'Guardado', detail: msg });
  }

  private showError(msg: string) {
    this.messageService.add({ severity: 'error', summary: 'Error', detail: msg });
  }
}