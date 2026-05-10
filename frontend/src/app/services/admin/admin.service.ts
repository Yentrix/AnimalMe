import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PublicationSummary } from '../publication/publication.service';

export interface AdminUser {
  id: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  role?: 'USER' | 'ADMIN';
  status?: 'ACTIVE' | 'BANNED_TEMPORARY' | 'BANNED_PERMANENT';
  bannedUntil?: string;
}

export interface AdminPet {
  id: number;
  name: string;
  species?: { name?: string };
  breed?: { name?: string };
  owner?: AdminUser;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) { }

  searchUsers(adminId: number, query: string): Observable<AdminUser[]> {
    let params = new HttpParams().set('adminId', adminId.toString());
    if (query.trim().length > 0) {
      params = params.set('query', query.trim());
    }
    return this.http.get<AdminUser[]>(`${this.apiUrl}/users`, { params });
  }

  banUser(adminId: number, userId: number, mode: 'TEMPORARY' | 'PERMANENT', hours?: number): Observable<AdminUser> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.put<AdminUser>(`${this.apiUrl}/users/${userId}/ban`, { mode, hours }, { params });
  }

  unbanUser(adminId: number, userId: number): Observable<AdminUser> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.put<AdminUser>(`${this.apiUrl}/users/${userId}/unban`, {}, { params });
  }

  listPublications(adminId: number): Observable<PublicationSummary[]> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.get<PublicationSummary[]>(`${this.apiUrl}/publications`, { params });
  }

  deletePublication(adminId: number, publicationId: number): Observable<void> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.delete<void>(`${this.apiUrl}/publications/${publicationId}`, { params });
  }

  listPets(adminId: number): Observable<AdminPet[]> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.get<AdminPet[]>(`${this.apiUrl}/pets`, { params });
  }

  deletePet(adminId: number, petId: number): Observable<void> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.delete<void>(`${this.apiUrl}/pets/${petId}`, { params });
  }

  sendNotification(adminId: number, payload: {
    title: string;
    message: string;
    sendToAll: boolean;
    userIds: number[];
  }): Observable<{ recipients: number }> {
    const params = new HttpParams().set('adminId', adminId.toString());
    return this.http.post<{ recipients: number }>(`${this.apiUrl}/notifications`, payload, { params });
  }
}
