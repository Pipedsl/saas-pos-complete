import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { BarcodeFormat } from '@zxing/library';
import { ZXingScannerModule } from '@zxing/ngx-scanner';

@Component({
  selector: 'app-barcode-scanner',
  imports: [CommonModule, ZXingScannerModule],
  templateUrl: './barcode-scanner.html',
  styleUrl: './barcode-scanner.css',
})
export class BarcodeScannerComponent {
  @Output() scanSuccess = new EventEmitter<string>();

  hasDevices: boolean = false;
  hasPermission: boolean = false;

  // Formatos permitidos (EAN-13 es el estándar de supermercado, QR, etc.)
  allowedFormats = [
    BarcodeFormat.QR_CODE,
    BarcodeFormat.EAN_13,
    BarcodeFormat.CODE_128,
    BarcodeFormat.DATA_MATRIX,
    BarcodeFormat.UPC_A,
    BarcodeFormat.EAN_8
  ];

  onCamerasFound(devices: MediaDeviceInfo[]): void {
    this.hasDevices = Boolean(devices && devices.length);
  }

  onHasPermission(has: boolean) {
    this.hasPermission = has;
    if (!has) {
      console.warn('No hay permisos de cámara');
    }
  }

  onCodeResult(resultString: string) {
    // Emitimos el código al padre (beep opcional)
    console.log('Código escaneado:', resultString);
    this.scanSuccess.emit(resultString);
  }

}
