import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { map, Observable } from "rxjs";
import { Product } from "../../core/models/product.model";

@Injectable({
    providedIn: 'root'
})
export class ProductsService {
    //Apunta a http://localhost:8081/api/products
    private apiUrl = `${environment.apiUrl}/api/products`;

    constructor(private http: HttpClient) { }

    getProducts(): Observable<Product[]> {
        return this.http.get<Product[]>(this.apiUrl);
    }

    createProduct(product: Partial<Product>): Observable<Product> {
        return this.http.post<Product>(this.apiUrl, product);
    }

    getProductBySku(sku: string): Observable<Product> {
        return this.http.get<Product>(`${this.apiUrl}/sku/${sku}`);
    }

    getProductById(id: string): Observable<Product> {
        return this.http.get<Product>(`${this.apiUrl}/${id}`);
    }

    updateProduct(id: string, product: Partial<Product>): Observable<Product> {
        return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
    }

    deleteProduct(id: string, force: boolean = false): Observable<any> {
        // Pasamos el query param ?force=true si el usuario elige borrar todo
        return this.http.delete(`${this.apiUrl}/${id}?force=${force}`);
    }

    searchProducts(query: string): Observable<Product[]> {
        // Obtenemos todo y filtramos en memoria (para catálogos < 1000 productos funciona bien)
        return this.http.get<Product[]>(this.apiUrl).pipe(
            map(products => products.filter(p =>
                // SKU: Usamos startsWith para que sea exacto al inicio (ej: "PAN" muestra "PAN801")
                p.sku.toLowerCase().startsWith(query.toLowerCase()) ||
                // NOMBRE: Usamos includes para que sea flexible
                p.name.toLowerCase().includes(query.toLowerCase())
            ))
        );
    }

    getLowStockProducts(): Observable<Product[]> {
        return this.http.get<Product[]>(`${this.apiUrl}/low-stock`);
    }
}