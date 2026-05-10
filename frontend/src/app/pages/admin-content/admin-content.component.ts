import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AdminPet, AdminService } from '../../services/admin/admin.service';
import { PublicationSummary } from '../../services/publication/publication.service';

@Component({
  selector: 'app-admin-content',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-content.component.html',
  styleUrl: './admin-content.component.css'
})
export class AdminContentComponent implements OnInit {
  tab: 'publications' | 'pets' = 'publications';
  publications: PublicationSummary[] = [];
  pets: AdminPet[] = [];
  error = '';

  constructor(private adminService: AdminService) { }

  ngOnInit(): void {
    this.loadPublications();
    this.loadPets();
  }

  loadPublications(): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    this.adminService.listPublications(adminId).subscribe({
      next: data => this.publications = data,
      error: () => this.error = 'No se pudieron cargar las publicaciones.'
    });
  }

  loadPets(): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    this.adminService.listPets(adminId).subscribe({
      next: data => this.pets = data,
      error: () => this.error = 'No se pudieron cargar las mascotas.'
    });
  }

  deletePublication(publicationId: number): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    this.adminService.deletePublication(adminId, publicationId).subscribe({
      next: () => this.loadPublications(),
      error: () => this.error = 'No se pudo borrar la publicación.'
    });
  }

  deletePet(petId: number): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    this.adminService.deletePet(adminId, petId).subscribe({
      next: () => this.loadPets(),
      error: () => this.error = 'No se pudo borrar la mascota.'
    });
  }

  private getCurrentUserId(): number | null {
    const raw = localStorage.getItem('user');
    if (!raw) {
      return null;
    }

    try {
      const user = JSON.parse(raw) as { id?: number };
      return user.id ?? null;
    } catch {
      return null;
    }
  }
}
