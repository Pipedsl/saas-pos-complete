import { Injectable } from '@angular/core';
import { initializeApp } from 'firebase/app';
import { getStorage, ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class ImageService {
    private storage;

    constructor() {
        // Inicializar Firebase solo una vez
        const app = initializeApp(environment.firebase);
        this.storage = getStorage(app);
    }

    // Método principal: Recibe archivo, comprime y sube
    async uploadImage(file: File, path: string = 'saas-pos/products'): Promise<string> {
        try {
            // 1. Convertir a WebP y comprimir
            const compressedFile = await this.compressImage(file);

            // 2. Crear referencia única (usamos timestamp para evitar duplicados)
            const fileName = `${Date.now()}_${file.name.split('.')[0]}.webp`;
            const storageRef = ref(this.storage, `${path}/${fileName}`);

            // 3. Subir
            const snapshot = await uploadBytes(storageRef, compressedFile);

            // 4. Obtener URL pública
            return await getDownloadURL(snapshot.ref);
        } catch (error) {
            console.error('Error subiendo imagen:', error);
            throw error;
        }
    }

    // Magia de compresión usando Canvas HTML5
    private compressImage(file: File): Promise<Blob> {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);

            reader.onload = (event: any) => {
                const img = new Image();
                img.src = event.target.result;

                img.onload = () => {
                    const canvas = document.createElement('canvas');
                    // Redimensionar si es muy grande (Max 1200px)
                    const MAX_WIDTH = 1200;
                    const scaleSize = MAX_WIDTH / img.width;
                    const width = (img.width > MAX_WIDTH) ? MAX_WIDTH : img.width;
                    const height = (img.width > MAX_WIDTH) ? (img.height * scaleSize) : img.height;

                    canvas.width = width;
                    canvas.height = height;

                    const ctx = canvas.getContext('2d');
                    ctx?.drawImage(img, 0, 0, width, height);

                    // Exportar como WebP con calidad 0.8 (80%)
                    canvas.toBlob((blob) => {
                        if (blob) {
                            resolve(blob);
                        } else {
                            reject(new Error('Error al comprimir imagen'));
                        }
                    }, 'image/webp', 0.8);
                };

                img.onerror = (error) => reject(error);
            };
        });
    }
}