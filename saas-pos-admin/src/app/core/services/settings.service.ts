import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SettingsService {
    private apiUrl = `${environment.apiUrl}/api/settings`;

    constructor(private http: HttpClient) { }

    getSettings(): Observable<any> {
        return this.http.get(this.apiUrl);
    }

    updateSettings(settings: any): Observable<any> {
        return this.http.post(this.apiUrl, settings);
    }
}