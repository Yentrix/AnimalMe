import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type NotificationType = 'ADOPTION_REQUEST_RECEIVED' | 'ADOPTION_REQUEST_ACCEPTED' | 'ADMIN_MESSAGE';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
  relatedPublicationId?: number;
  relatedRequestId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;

  constructor(private http: HttpClient) { }

  getNotifications(userId: number): Observable<AppNotification[]> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.get<AppNotification[]>(this.apiUrl, { params });
  }

  markAsRead(notificationId: number, userId: number): Observable<AppNotification> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.put<AppNotification>(`${this.apiUrl}/${notificationId}/read`, {}, { params });
  }

  markAllAsRead(userId: number): Observable<void> {
    const params = new HttpParams().set('userId', userId.toString());
    return this.http.put<void>(`${this.apiUrl}/read-all`, {}, { params });
  }
}
