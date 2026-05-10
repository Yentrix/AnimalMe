import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PublicationService {
  private apiUrl = `${environment.apiUrl}/publications`;

  constructor(private http: HttpClient) { }

  createPublication(formData: FormData, authorId: number): Observable<any> {
    const params = new HttpParams().set('authorId', authorId.toString());
    return this.http.post<any>(this.apiUrl, formData, { params });
  }
}
