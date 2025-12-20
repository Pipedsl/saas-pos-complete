import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';

// Servicios y Modelos
import { ProductsService } from '../../../core/services/products.service';
import { CategoriesService } from '../../../core/services/categories.service';
import { SuppliersService } from '../../../core/services/suppliers.service';
import { Category } from '../../../core/models/category.model';
import { AutoCompleteModule, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { PrimeImportsModule } from '../../../prime-imports';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { Product } from '../../../core/models/product.model';
import { Supplier } from '../../../core/models/supplier.model';
import { BarcodeScannerComponent } from '../../../shared/components/barcode-scanner/barcode-scanner';

@Component({
  selector: 'app-product-form',
  imports: [CommonModule, ReactiveFormsModule, AutoCompleteModule, PrimeImportsModule, FormsModule, ConfirmDialogModule, BarcodeScannerComponent, RouterModule],
  providers: [ConfirmationService],
  templateUrl: './product-form.html',
  styleUrl: './product-form.css',
})
export class ProductFormComponent implements OnInit {
  productForm: FormGroup;

  categories: Category[] = [];         // Lista maestra (todas)
  filteredCategories: Category[] = []; // Lista filtrada (lo que se muestra al escribir)

  // Proveedores (Simulados por ahora para que compile, luego conectamos API)
  suppliers: Supplier[] = [];
  filteredSuppliers: Supplier[] = [];

  loading = false;

  showCategoryDialog = false;
  showSupplierDialog = false;
  newCategoryName = '';
  newSupplierName = '';

  // Objetos temporales para los formularios rápidos
  quickCategory = { name: '', description: '' };
  quickSupplier = { name: '', rut: '', contactName: '', contactEmail: '', contactPhone: '' }; // Campos clave para Chile

  unitOptions = [
    { label: 'Por Unidad (c/u)', value: 'UNIT' },
    { label: 'Por Kilo (Granel)', value: 'KG' }
  ];

  // Para manejar los atributos dinámicos en la tabla visual
  attributesList: any[] = [];

  isEditMode = false;
  productId: string | null = null;

  suggestedProducts: Product[] = [];

  showScanner = false;

  constructor(
    private fb: FormBuilder,
    private productsService: ProductsService,
    private categoriesService: CategoriesService,
    private confirmationService: ConfirmationService,
    private suppliersService: SuppliersService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.productForm = this.fb.group({
      sku: ['', Validators.required],
      name: ['', Validators.required],
      description: [''],
      selectedCategory: [null, Validators.required],
      selectedSupplier: [null], // Opcional por ahora
      measurementUnit: ['UNIT', Validators.required],
      stockCurrent: [0, [Validators.required, Validators.min(0)]],
      stockMin: [5],

      costPrice: [0, [Validators.required, Validators.min(0)]],
      finalPrice: [0],
      isTaxIncluded: [true], // Checkbox por defecto activado
      taxPercent: [19],

      priceNeto: [0],
      marginPercent: [0],

    });
  }

  ngOnInit() {
    this.loadCategories();
    this.loadSuppliers();
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.isEditMode = true;
        this.productId = id;
        this.loadProductData(id);
      } else {
        // 2. Si es modo CREAR, activamos el detector de SKU
        // this.listenToSkuChanges();
      }
    });
  }

  handleScan(code: string) {
    console.log('Recibido en form:', code);
    this.productForm.patchValue({ sku: code }); // Poner código en el input
    this.showScanner = false; // Cerrar cámara automáticamente
    // Opcional: Reproducir sonido de éxito aquí
  }

  loadProductData(id: string) {
    this.loading = true; // Buen feedback visual
    this.productsService.getProductById(id).subscribe({
      next: (product) => {
        this.loading = false;

        // 1. Rellenar campos simples
        this.productForm.patchValue({
          sku: product.sku,
          name: product.name,
          description: product.description,
          stockCurrent: product.stockCurrent, // <--- Recuperar Stock
          stockMin: product.stockMin,
          measurementUnit: product.measurementUnit, // <--- Recuperar Tipo Venta (KG/UNIT)

          costPrice: product.costPrice,
          taxPercent: product.taxPercent,

          finalPrice: product.priceFinal, // Helper para visual
          isTaxIncluded: product.isTaxIncluded,

          priceNeto: product.priceNeto,
          marginPercent: product.marginPercent,

        });

        // 2. Recuperar Categoría (Objeto completo para el Autocomplete)
        if (product.categoryId && product.categoryName) {
          this.productForm.patchValue({
            selectedCategory: { id: product.categoryId, name: product.categoryName }
          });
        }

        this.calculateMath();

        if (product.supplierId && product.supplierName) {
          this.productForm.patchValue({
            selectedSupplier: { id: product.supplierId, name: product.supplierName } // Objeto para AutoComplete
          });
        }

        // 3. Recuperar Atributos Dinámicos (JSON -> Tabla)
        this.attributesList = [];
        if (product.attributes) {
          Object.keys(product.attributes).forEach(key => {
            this.attributesList.push({ key: key, value: product.attributes![key] });
          });
        }

        // 4. Bloquear SKU en edición
        this.productForm.get('sku')?.disable();

      },
      error: (err) => {
        console.error(err);
        this.loading = false;
        alert('Error al cargar producto');
        this.router.navigate(['/products']);
      }
    });
  }

  // Helper necesario si no lo tienes
  getPriceWithTax(p: Product): number {
    return p.priceNeto * (1 + (p.taxPercent || 19) / 100);
  }

  filterSkus(event: any) {
    const query = event.query;
    this.productsService.searchProducts(query).subscribe(data => {
      this.suggestedProducts = data;
    });
  }

  // 2. MÉTODO AL SELECCIONAR UN SKU EXISTENTE
  onSkuSelect(event: AutoCompleteSelectEvent) {
    const product = event.value; // El producto seleccionado

    // Lanzar el Diálogo Bonito
    this.confirmationService.confirm({
      message: `El producto "${product.name}" ya existe con el SKU ${product.sku}. ¿Quieres editarlo?`,
      header: 'Producto Existente',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Sí, Editar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-primary',
      rejectButtonStyleClass: 'p-button-text p-button-secondary',
      accept: () => {
        // Redirigir a modo edición
        this.router.navigate(['/products/edit', product.id]);
      },
      reject: () => {
        // Limpiar el campo si cancela
        this.productForm.get('sku')?.setValue('');
      }
    });
  }

  loadCategories() {
    this.categoriesService.getCategories().subscribe({
      next: (data) => this.categories = data,
      error: (err) => console.error('Error cargando categorías', err)
    });
  }

  // --- LÓGICA DEL AUTOCOMPLETE ---
  filterCategories(event: AutoCompleteCompleteEvent) {
    let query = event.query.toLowerCase();

    // Filtramos la lista maestra buscando coincidencias por nombre
    this.filteredCategories = this.categories.filter(category =>
      category.name.toLowerCase().includes(query)
    );
  }

  filterSuppliers(event: AutoCompleteCompleteEvent) {
    const query = event.query.toLowerCase();
    this.filteredSuppliers = this.suppliers.filter(s => s.name.toLowerCase().includes(query));
  }

  loadSuppliers() {
    this.suppliersService.getSuppliers().subscribe({
      next: (data) => this.suppliers = data,
      error: (err) => console.error('Error cargando proveedores', err)
    });
  }

  calculateMath() {
    // 1. Obtener valores del formulario
    const cost = this.productForm.get('costPrice')?.value || 0;
    const finalPrice = this.productForm.get('finalPrice')?.value || 0;
    const isTaxIncluded = this.productForm.get('isTaxIncluded')?.value;
    const taxPercent = this.productForm.get('taxPercent')?.value || 19;

    let neto = 0;

    // 2. CALCULAR PRECIO NETO (Hacia atrás)
    if (isTaxIncluded) {
      // Si el precio final (ej: 1190) YA TIENE IVA, dividimos por 1.19
      neto = finalPrice / (1 + (taxPercent / 100));
    } else {
      // Si NO tiene IVA, el precio final ES el neto
      neto = finalPrice;
    }

    // 3. CALCULAR MARGEN DE GANANCIA
    // Fórmula: ((VentaNeto - Costo) / Costo) * 100
    let margin = 0;
    if (cost > 0) {
      margin = ((neto - cost) / cost) * 100;
    } else if (neto > 0) {
      margin = 100; // Si costó 0 y vendemos a algo, ganancia total
    }

    // 4. Actualizar campos calculados (sin emitir evento para no buclear)
    this.productForm.patchValue({
      priceNeto: Math.round(neto), // Guardamos entero
      marginPercent: parseFloat(margin.toFixed(2)) // 2 decimales visuales
    }, { emitEvent: false });
  }

  // Helper para el click en el label "Incluye IVA"
  toggleTax() {
    const current = this.productForm.get('isTaxIncluded')?.value;
    this.productForm.patchValue({ isTaxIncluded: !current });
    this.calculateMath();
  }

  // GUARDAR CATEGORÍA RÁPIDA (Actualizado)
  saveQuickCategory() {
    if (!this.quickCategory.name.trim()) return;

    const newCat: Category = {
      name: this.quickCategory.name,
      description: this.quickCategory.description
    };

    this.categoriesService.createCategory(newCat).subscribe(saved => {
      this.categories.push(saved);
      this.productForm.patchValue({ selectedCategory: saved });
      this.showCategoryDialog = false;
      this.quickCategory = { name: '', description: '' }; // Limpiar
    });
  }

  saveQuickSupplier() {
    if (!this.quickSupplier.name.trim()) return;

    const newSup: Supplier = {
      name: this.quickSupplier.name,
      rut: this.quickSupplier.rut,
      contactName: this.quickSupplier.contactName,
      contactEmail: this.quickSupplier.contactEmail,
      contactPhone: this.quickSupplier.contactPhone
    };

    this.suppliersService.createSupplier(newSup).subscribe({
      next: (saved) => {
        this.suppliers.push(saved); // Agregamos el REAL que volvió de la BD
        this.productForm.patchValue({ selectedSupplier: saved }); // Lo seleccionamos
        this.showSupplierDialog = false;
        // Limpiar formulario
        this.quickSupplier = { name: '', rut: '', contactName: '', contactEmail: '', contactPhone: '' };
      },
      error: (err) => {
        console.error(err);
        alert('Error al guardar proveedor');
      }
    });
  }

  addAttribute() {
    this.attributesList.push({ key: '', value: '' });
  }

  removeAttribute(index: number) {
    this.attributesList.splice(index, 1);
  }

  onSubmit() {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched(); // <--- ESTA ES LA CLAVE
      // Opcional: Mostrar un mensaje general
      // this.messageService.add({severity:'error', summary:'Error', detail:'Completa los campos obligatorios'});
      return;
    };
    this.loading = true;

    // IMPORTANTE: Usamos getRawValue() para obtener el SKU aunque esté deshabilitado (disabled)
    const formValue = this.productForm.getRawValue();

    // 1. Procesar Atributos Dinámicos (Tu lógica original estaba bien)
    const attributesJson: any = {};
    this.attributesList.forEach(attr => {
      if (attr.key && attr.value) {
        attributesJson[attr.key] = attr.value;
      }
    });

    // 2. Construir el Payload (Datos para el Backend)
    const productPayload = {
      sku: formValue.sku,
      name: formValue.name,
      description: formValue.description,

      // Precios y Stock
      costPrice: formValue.costPrice,
      priceFinal: formValue.finalPrice,
      isTaxIncluded: formValue.isTaxIncluded,
      taxPercent: formValue.taxPercent,

      priceNeto: formValue.priceNeto,


      stockCurrent: formValue.stockCurrent,
      stockMin: formValue.stockMin,
      measurementUnit: formValue.measurementUnit,

      // Relaciones y JSON
      attributes: attributesJson,
      categoryId: formValue.selectedCategory?.id, // Extraer solo el ID
      supplierId: formValue.selectedSupplier?.id, // (Pendiente hasta que tengas proveedores)
    };

    // 3. DECISIÓN: ¿CREAR O ACTUALIZAR?
    if (this.isEditMode && this.productId) {

      // --- MODO EDICIÓN (UPDATE) ---
      this.productsService.updateProduct(this.productId, productPayload).subscribe({
        next: () => {
          this.loading = false;
          // Mensaje de éxito opcional (podrías usar MessageService aquí)
          alert('Producto actualizado correctamente');
          this.router.navigate(['/products']);
        },
        error: (err) => {
          console.error('Error actualizando:', err);
          this.loading = false;
          alert('Error al actualizar. Revisa consola.');
        }
      });

    } else {

      // --- MODO CREACIÓN (CREATE) ---
      this.productsService.createProduct(productPayload).subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/products']);
        },
        error: (err) => {
          console.error('Error creando:', err);
          this.loading = false;
          alert('Error al crear. Revisa consola.');
        }
      });
    }
  }

}
