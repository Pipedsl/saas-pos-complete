import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PrimeImportsModule } from '../../prime-imports';
import { MessageService } from 'primeng/api';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';



@Component({
  selector: 'app-settings',

  imports: [CommonModule, FormsModule, PrimeImportsModule],
  providers: [MessageService],
  templateUrl: './settings.html',
})
export class SettingsComponent implements OnInit {
  settings: any = {
    shopName: '',
    shopRut: '',
    shopAddress: '',
    footerMessage: '',
    printerType: '58mm', // Valor por defecto importante
    ticketPrefix: '',
    bankName: '',
    accountType: '',
    accountNumber: '',
    rut: '',
    email: ''
  };

  constructor(private http: HttpClient, private messageService: MessageService) { }

  ngOnInit() {
    this.loadSettings();
  }

  loadSettings() {
    this.http.get(`${environment.apiUrl}/api/settings`).subscribe({
      next: (data: any) => {
        // Si viene data del backend, la usamos. Si no, mantenemos el objeto con defaults.
        // El spread operator (...) fusiona los defaults con lo que venga del server.
        this.settings = { ...this.settings, ...(data || {}) };

        // Asegurar que printerType tenga un valor válido si viene vacío
        if (!this.settings.printerType) {
          this.settings.printerType = '58mm';
        }
      },
      error: (err) => console.error('Error cargando configuración', err)
    });
  }

  saveSettings() {
    this.http.post(`${environment.apiUrl}/api/settings`, this.settings).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Guardado',
          detail: 'Configuración actualizada correctamente'
        });
      },
      error: (err) => {
        console.error(err);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo guardar la configuración'
        });
      }
    });
  }

}
