import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { MessageService } from 'primeng/api';
import { PublicShop, StoreService } from '../../../../core/services/store.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CartItem, CartService } from '../../../../core/services/cart.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PrimeImportsModule } from '../../../../prime-imports';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule, PrimeImportsModule, RouterModule,
    ReactiveFormsModule],
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

  constructor(
    private cartService: CartService,
    private storeService: StoreService,
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private messageService: MessageService
  ) {
    // Formulario de datos del cliente
    this.checkoutForm = this.fb.group({
      customerName: ['', [Validators.required, Validators.minLength(3)]],
      customerPhone: ['', [Validators.required, Validators.pattern('^[0-9+ ]{8,15}$')]], // Validación simple de teléfono
      deliveryMethod: ['PICKUP', Validators.required], // PICKUP o DELIVERY
      customerAddress: [''], // Requerido solo si es DELIVERY
      paymentMethod: ['', Validators.required], // CASH, TRANSFER, etc.
      notes: ['']
    });
  }

  ngOnInit() {
    this.cartItems$ = this.cartService.items$;
    this.total$ = this.cartService.total$;
    // Obtener slug del padre
    this.route.parent?.paramMap.subscribe(params => {
      this.slug = params.get('slug') || '';
      if (this.slug) {
        this.loadShopConfig();
      }
    });

    // Validar dirección según entrega
    this.checkoutForm.get('deliveryMethod')?.valueChanges.subscribe(val => {
      const addressControl = this.checkoutForm.get('customerAddress');
      if (val === 'DELIVERY') {
        addressControl?.setValidators([Validators.required, Validators.minLength(5)]);
      } else {
        addressControl?.clearValidators();
      }
      addressControl?.updateValueAndValidity();
    });
  }

  loadShopConfig() {
    this.storeService.getShopInfo(this.slug).subscribe(config => {
      this.shopConfig = config;
      // Preseleccionar primer método de pago disponible si existe
      // (Lógica opcional para mejorar UX)
    });
  }

  increase(productId: string) {
    this.cartService.updateQuantity(productId, 1);
  }

  decrease(productId: string) {
    this.cartService.updateQuantity(productId, -1);
  }

  remove(productId: string) {
    this.cartService.removeFromCart(productId);
  }

  submitOrder() {
    if (this.checkoutForm.invalid) {
      this.checkoutForm.markAllAsTouched();
      this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Completa los campos obligatorios' });
      return;
    }

    this.loading = true;

    // Preparar el objeto para el Backend
    // (Angular maneja los Observables, necesitamos el valor actual del carrito)
    const cartItems = this.cartService.getCurrentItems().map(item => ({
      productId: item.product.id,
      quantity: item.quantity
    }));

    const orderPayload = {
      ...this.checkoutForm.value,
      items: cartItems
    };

    this.storeService.createOrder(this.slug, orderPayload).subscribe({
      next: (res) => {
        this.loading = false;
        this.messageService.add({ severity: 'success', summary: '¡Éxito!', detail: 'Pedido #' + res.orderNumber + ' creado.' });

        this.cartService.clearCart(); // Vaciar carrito

        // Opcional: Redirigir a WhatsApp
        // this.redirectToWhatsApp(res.orderNumber); 

        this.router.navigate(['/store', this.slug]); // Volver al inicio
      },
      error: (err) => {
        this.loading = false;
        console.error(err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: err.error?.error || 'No se pudo crear el pedido' });
      }
    });
  }
}
