import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MessageService } from 'primeng/api';
import { PublicShop, StoreService } from '../../../../core/services/store.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CartItem, CartService } from '../../../../core/services/cart.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PrimeImportsModule } from '../../../../prime-imports';
import { Observable, take } from 'rxjs';
import { REGIONES_Y_COMUNAS } from '../../../../core/utils/chile-data'; // <--- IMPORTANTE
import { ShopConfigService } from '../../../../core/services/shop-config.service';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule, PrimeImportsModule, RouterModule, ReactiveFormsModule],
  providers: [MessageService],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css',
})
export class CheckoutComponent implements OnInit {
  cartItems$!: Observable<CartItem[]>;
  total$!: Observable<number>;

  shopConfig: PublicShop | null = null;
  slug: string = '';
  checkoutForm: FormGroup;
  loading = false;

  // Datos Geográficos
  regiones = REGIONES_Y_COMUNAS;
  comunasDisponibles: string[] = [];

  courierNames: { [key: string]: string } = {
    'starken': 'Starken',
    'chilexpress': 'Chilexpress',
    'varmontt': 'Varmontt',
    'blue_express': 'Blue Express',
    'correos': 'Correos de Chile'
  };

  availableCouriers: string[] = [];

  constructor(
    private cartService: CartService,
    private storeService: StoreService,
    private shopConfigService: ShopConfigService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private messageService: MessageService
  ) {
    this.checkoutForm = this.fb.group({
      customerName: ['', [Validators.required, Validators.minLength(3)]],
      customerRut: ['', [Validators.required, Validators.pattern(/^[0-9]+-[0-9kK]{1}$/)]], // Validación simple formato RUT
      customerPhone: ['', [Validators.required, Validators.pattern(/^[0-9+ ]{8,15}$/)]],
      customerEmail: ['', [Validators.email]], // Email opcional pero con formato válido

      deliveryMethod: ['PICKUP', Validators.required],

      // Dirección Estructurada
      region: [''],
      commune: [''],
      street: [''], // Calle y número

      courier: [''],
      paymentMethod: ['', Validators.required],
      notes: ['']
    });
  }

  ngOnInit() {
    this.cartItems$ = this.cartService.items$;
    this.total$ = this.cartService.total$;

    this.route.parent?.paramMap.subscribe(params => {
      this.slug = params.get('slug') || '';
      if (this.slug) {
        this.loadShopConfig();
      }
    });

    // 1. Lógica de Regiones y Comunas
    this.checkoutForm.get('region')?.valueChanges.subscribe(regionName => {
      this.updateCommunes(regionName);
    });

    // 2. Validaciones dinámicas según Delivery
    this.checkoutForm.get('deliveryMethod')?.valueChanges.subscribe(method => {
      const regionCtrl = this.checkoutForm.get('region');
      const communeCtrl = this.checkoutForm.get('commune');
      const streetCtrl = this.checkoutForm.get('street');
      const courierCtrl = this.checkoutForm.get('courier');

      if (method === 'DELIVERY') {
        regionCtrl?.setValidators(Validators.required);
        communeCtrl?.setValidators(Validators.required);
        streetCtrl?.setValidators([Validators.required, Validators.minLength(5)]);
        courierCtrl?.setValidators(Validators.required);
      } else {
        regionCtrl?.clearValidators();
        communeCtrl?.clearValidators();
        streetCtrl?.clearValidators();
        courierCtrl?.clearValidators();
      }

      regionCtrl?.updateValueAndValidity();
      communeCtrl?.updateValueAndValidity();
      streetCtrl?.updateValueAndValidity();
      courierCtrl?.updateValueAndValidity();
    });
  }

  updateCommunes(regionName: string) {
    const regionData = this.regiones.find(r => r.region === regionName);
    this.comunasDisponibles = regionData ? regionData.comunas : [];
    // Resetear la comuna al cambiar región
    this.checkoutForm.patchValue({ commune: '' });
  }

  loadShopConfig() {
    this.storeService.getShopInfo(this.slug).subscribe(config => {
      this.shopConfig = config;

      // --- CORRECCIÓN: Mover datos anidados a la raíz para que el HTML los vea ---
      if (config.shippingMethods) {
        // El backend los envía dentro de shippingMethods, los copiamos afuera
        this.shopConfig.recommendedCourier = config.shippingMethods['recommendedCourier'];
        this.shopConfig.dispatchDays = config.shippingMethods['dispatchDays'];
      }
      // ---------------------------------------------------------------------------

      if (config.shippingMethods && Array.isArray(config.shippingMethods['companies'])) {
        this.availableCouriers = config.shippingMethods['companies'];
      } else {
        this.availableCouriers = [];
      }
    });
  }

  increase(productId: string) { this.cartService.updateQuantity(productId, 1); }
  decrease(productId: string) { this.cartService.updateQuantity(productId, -1); }
  remove(productId: string) { this.cartService.removeFromCart(productId); }

  submitOrder() {
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos obligatorios' });
      return;
    }

    this.loading = true;
    const currentItems = this.cartService.getCurrentItems();

    const cartItemsPayload = currentItems.map(item => ({
      productId: item.product.id,
      quantity: item.quantity
    }));

    const formVal = this.checkoutForm.value;

    // Mapeamos el formulario al DTO del Backend (WebOrderRequest)
    const orderPayload = {
      customerName: formVal.customerName,
      customerPhone: formVal.customerPhone,
      customerEmail: formVal.customerEmail,
      customerRut: formVal.customerRut, // <--- NUEVO

      deliveryMethod: formVal.deliveryMethod,
      courier: formVal.courier,
      paymentMethod: formVal.paymentMethod,
      notes: formVal.notes,

      // Dirección desglosada
      region: formVal.region,
      commune: formVal.commune,
      streetAndNumber: formVal.street, // <--- Ojo, en el backend lo llamamos streetAndNumber en el DTO

      items: cartItemsPayload
    };

    this.storeService.createOrder(this.slug, orderPayload).subscribe({
      next: (res) => {
        this.loading = false;
        this.messageService.add({ severity: 'success', summary: 'Pedido Creado', detail: `Orden #${res.orderNumber}` });

        // Redirigir a WhatsApp
        this.redirectToWhatsApp(res.orderNumber, formVal, currentItems);

        this.cartService.clearCart();
        this.router.navigate(['/store', this.slug]);
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.error || 'No se pudo crear el pedido' });
      }
    });
  }

  selectCourier(code: string) {
    this.checkoutForm.patchValue({ courier: code });
    this.checkoutForm.get('courier')?.markAsTouched();
  }

  redirectToWhatsApp(orderNumber: string, formData: any, items: CartItem[]) {
    const phone = this.shopConfig?.contactPhone || '56900000000';
    let total = 0;
    this.total$.pipe(take(1)).subscribe(t => total = t);

    // Construcción del mensaje
    let text = `🛒 *NUEVO PEDIDO WEB #${orderNumber}*\n`;
    text += `👤 *Cliente:* ${formData.customerName}\n`;
    text += `🆔 *RUT:* ${formData.customerRut}\n`;
    if (formData.customerEmail) text += `📧 ${formData.customerEmail}\n`;
    text += `📞 ${formData.customerPhone}\n\n`;

    text += `📋 *Detalle:*\n`;
    items.forEach(item => {
      const idRef = item.product.sku ? `(SKU: ${item.product.sku})` : '';
      text += `▪️ ${item.quantity}x ${item.product.name} ${idRef}\n`;
    });

    text += `\n💰 *Total: $${total.toLocaleString('es-CL')}*\n`;
    text += `--------------------------------\n`;

    if (formData.deliveryMethod === 'DELIVERY') {
      const courierName = this.courierNames[formData.courier] || formData.courier;
      text += `🚚 *Despacho:* ${courierName}\n`;
      text += `📍 *Dirección:* ${formData.street}, ${formData.commune}, ${formData.region}\n`;
    } else {
      text += `🏪 *Entrega:* Retiro en Tienda\n`;
    }

    const paymentLabel = formData.paymentMethod === 'TRANSFER' ? 'Transferencia' :
      formData.paymentMethod === 'CASH' ? 'Efectivo' : 'Cobro Carrier';
    text += `💳 *Pago:* ${paymentLabel}\n`;

    if (formData.notes) {
      text += `📝 *Nota:* ${formData.notes}\n`;
    }

    const url = `https://wa.me/${phone}?text=${encodeURIComponent(text)}`;
    window.open(url, '_blank');
  }
}