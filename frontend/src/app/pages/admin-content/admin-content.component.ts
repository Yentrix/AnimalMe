import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminPet, AdminService, PetDeletionImpact } from '../../services/admin/admin.service';
import { PublicationSummary } from '../../services/publication/publication.service';

@Component({
  selector: 'app-admin-content',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-content.component.html',
  styleUrl: './admin-content.component.css'
})
export class AdminContentComponent implements OnInit {
  tab: 'publications' | 'pets' = 'publications';
  publications: PublicationSummary[] = [];
  pets: AdminPet[] = [];
  publicationQuery = '';
  petQuery = '';
  error = '';
  showDeleteModal = false;
  deleteTargetType: 'publication' | 'pet' | null = null;
  deleteTargetId: number | null = null;
  deleteTargetName = '';
  deleteLinkedPublicationsCount = 0;
  deleteLinkedPublicationTitles: string[] = [];
  isDeleteLoading = false;

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

    this.adminService.listPublications(adminId, this.publicationQuery).subscribe({
      next: data => this.publications = data,
      error: () => this.error = 'No se pudieron cargar las publicaciones.'
    });
  }

  loadPets(): void {
    const adminId = this.getCurrentUserId();
    if (!adminId) {
      return;
    }

    this.adminService.listPets(adminId, this.petQuery).subscribe({
      next: data => this.pets = data,
      error: () => this.error = 'No se pudieron cargar las mascotas.'
    });
  }

  searchCurrentTab(): void {
    if (this.tab === 'publications') {
      this.loadPublications();
      return;
    }

    this.loadPets();
  }

  deletePublication(publicationId: number): void {
    const publication = this.publications.find(item => item.id === publicationId);
    if (!publication) {
      return;
    }

    this.deleteTargetType = 'publication';
    this.deleteTargetId = publicationId;
    this.deleteTargetName = publication.title || 'publicacion';
    this.deleteLinkedPublicationsCount = 0;
    this.deleteLinkedPublicationTitles = [];
    this.showDeleteModal = true;
  }

  deletePet(petId: number): void {
    const adminId = this.getCurrentUserId();
    const pet = this.pets.find(item => item.id === petId);
    if (!adminId || !pet) {
      return;
    }

    this.isDeleteLoading = true;
    this.error = '';
    this.deleteTargetType = 'pet';
    this.deleteTargetId = petId;
    this.deleteTargetName = pet.name || 'mascota';
    this.deleteLinkedPublicationsCount = 0;
    this.deleteLinkedPublicationTitles = [];
    this.showDeleteModal = true;

    this.adminService.getPetDeletionImpact(adminId, petId).subscribe({
      next: (impact: PetDeletionImpact) => {
        this.deleteLinkedPublicationsCount = impact.linkedPublicationsCount || 0;
        this.deleteLinkedPublicationTitles = impact.publicationTitles || [];
        this.isDeleteLoading = false;
      },
      error: () => {
        this.error = 'No se pudo comprobar el impacto del borrado de la mascota.';
        this.isDeleteLoading = false;
      }
    });
  }

  confirmDelete(): void {
    const adminId = this.getCurrentUserId();
    if (!adminId || !this.deleteTargetType || !this.deleteTargetId) {
      return;
    }

    this.error = '';
    this.isDeleteLoading = true;

    if (this.deleteTargetType === 'publication') {
      this.adminService.deletePublication(adminId, this.deleteTargetId).subscribe({
        next: () => {
          this.closeDeleteModal();
          this.loadPublications();
        },
        error: () => {
          this.error = 'No se pudo borrar la publicacion.';
          this.isDeleteLoading = false;
        }
      });
      return;
    }

    this.adminService.deletePet(adminId, this.deleteTargetId).subscribe({
      next: () => {
        this.closeDeleteModal();
        this.loadPets();
        this.loadPublications();
      },
      error: () => {
        this.error = 'No se pudo borrar la mascota.';
        this.isDeleteLoading = false;
      }
    });
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.deleteTargetType = null;
    this.deleteTargetId = null;
    this.deleteTargetName = '';
    this.deleteLinkedPublicationsCount = 0;
    this.deleteLinkedPublicationTitles = [];
    this.isDeleteLoading = false;
  }

  get deleteModalTitle(): string {
    if (this.deleteTargetType === 'pet') {
      return 'Confirmar borrado de mascota';
    }
    return 'Confirmar borrado de publicacion';
  }

  get deleteModalMessage(): string {
    if (this.deleteTargetType === 'pet') {
      return `Vas a borrar la mascota \"${this.deleteTargetName}\".`;
    }
    return `Vas a borrar la publicacion \"${this.deleteTargetName}\".`;
  }

  getPublicationThumbnailUrl(publication: PublicationSummary): string {
    const publicationImage = publication.images?.[0]?.url;
    if (publicationImage && publicationImage.trim().length > 0) {
      return publicationImage;
    }

    const petImage = publication.pets?.[0]?.images?.[0]?.url;
    if (petImage && petImage.trim().length > 0) {
      return petImage;
    }

    return 'assets/placeholder.png';
  }

  getPetThumbnailUrl(pet: AdminPet): string {
    const petImage = pet.images?.[0]?.url;
    if (petImage && petImage.trim().length > 0) {
      return petImage;
    }

    return 'assets/placeholder.png';
  }

  handleImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'assets/placeholder.png';
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
