import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserProfile {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  contactEmail?: string;
  contactPhone?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  getUserById(id: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.apiUrl}/${id}`);
  }

  updateProfile(id: number, payload: {
    firstName: string;
    email: string;
    contactEmail: string;
    contactPhone: string;
  }): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.apiUrl}/${id}`, payload);
  }

  updatePassword(id: number, payload: {
    currentPassword: string;
    newPassword: string;
    confirmNewPassword: string;
  }): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/password`, payload);
  }
}
