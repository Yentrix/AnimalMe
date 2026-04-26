import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private userId: number | null = null;

  constructor(private http: HttpClient) { }

  login(credentials: any): Observable<any> {
    return this.http.post(this.apiUrl + '/users/login/client', credentials)
  }

  register(credentials: any): Observable<any> {
    return this.http.post(this.apiUrl + '/users/register/client', credentials)
  }

  setUserId(id: number) {
    this.userId = id;
  }

  getUserId(): number | null {
    return this.userId;
  }

  logout() {
    this.userId = null;
  }
}
