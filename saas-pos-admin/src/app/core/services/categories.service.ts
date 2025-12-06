import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Category } from '../models/category.model';

@Injectable({
    providedIn: 'root'
})
export class CategoriesService {
    private apiUrl = `${environment.apiUrl}/api/categories`;

    constructor(private http: HttpClient) { }

    getCategories(): Observable<Category[]> {
        return this.http.get<Category[]>(this.apiUrl);
    }

    createCategory(category: Category): Observable<Category> {
        return this.http.post<Category>(this.apiUrl, category);
    }
}