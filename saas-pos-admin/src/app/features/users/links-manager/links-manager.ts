import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { DemoLink, SalesAgentService } from '../services/sales-agent.service';
import { CommonModule } from '@angular/common';
import { PrimeImportsModule } from '../../../prime-imports';

@Component({
  selector: 'app-links-manager',
  imports: [CommonModule, PrimeImportsModule],
  providers: [MessageService],
  templateUrl: './links-manager.html',
  styleUrl: './links-manager.css',
})
export class LinksManagerComponent {

  generatedLinks: DemoLink[] = [];
  loading = false;

  constructor(
    private salesService: SalesAgentService,
    private messageService: MessageService
  ) { }

  generateNew() {
    this.loading = true;
    this.salesService.generateLink().subscribe({
      next: (link) => {
        this.generatedLinks.unshift(link);
        this.loading = false;
        this.messageService.add({
          severity: 'success',
          summary: 'Listo',
          detail: 'Link generado correctamente'
        });
      },
      error: (err) => {
        this.loading = false;
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'No se pudo generar el link. Intenta nuevamente.'
        });
      }
    })
  }

  copyToClipboard(url: string) {
    navigator.clipboard.writeText(url).then(() => {
      this.messageService.add({
        severity: 'success',
        summary: 'Copiado',
        detail: 'El enlace ha sido copiado al portapapeles'
      });
    })
  }



}
