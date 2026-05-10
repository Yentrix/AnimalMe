import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdoptionStatus } from '../../enums/adoption-status';
import { PetService } from '../../services/pet/pet.service';
import {
  AdoptionRequestSummary,
  PublicationService,
  PublicationSummary
} from '../../services/publication/publication.service';

interface PetImage {
  url: string;
}

interface PetSummary {
  id: number;
  name: string;
  species?: { name?: string };
  breed?: { name?: string };
  images?: PetImage[];
}

interface PreviewImage {
  url: string;
  name: string;
  source: 'pet' | 'upload';
}

interface AuthUser {
  id?: number;
}

type PublicationModalView = 'menu' | 'edit' | 'requests';

@Component({
  selector: 'app-post-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './post-create.component.html',
  styleUrl: './post-create.component.css'
})
export class PostCreateComponent implements OnInit {
  postForm!: FormGroup;
  publicationEditForm!: FormGroup;
  pets: PetSummary[] = [];
  selectedPetIds: number[] = [];
  selectedFiles: File[] = [];
  previewImages: PreviewImage[] = [];
  publications: PublicationSummary[] = [];
  publicationImageIndex: Record<number, number> = {};
  selectedPublication: PublicationSummary | null = null;
  publicationModalView: PublicationModalView = 'menu';
  isPublicationModalOpen = false;
  adoptionRequests: AdoptionRequestSummary[] = [];
  isLoadingRequests = false;
  publicationActionMessage = '';
  publicationActionError = '';
  isSubmitting = false;
  isSavingPublication = false;
  errorMessage = '';
  successMessage = '';
  adoptionStatuses = Object.values(AdoptionStatus);
  readonly statusLabels: Record<AdoptionStatus, string> = {
    [AdoptionStatus.AVAILABLE]: 'Se puede adoptar',
    [AdoptionStatus.URGENT]: 'Urgente de adopcion',
    [AdoptionStatus.ADOPTED]: 'Adoptado'
  };
  readonly requestStatusLabels: Record<AdoptionRequestSummary['status'], string> = {
    PENDING: 'Pendiente',
    ACCEPTED: 'Aceptada',
    REJECTED: 'Rechazada',
    ARCHIVED: 'Archivada'
  };

  constructor(
    private fb: FormBuilder,
    private petService: PetService,
    private publicationService: PublicationService
  ) { }

  ngOnInit(): void {
    this.postForm = this.fb.group({
      title: [''],
      description: [''],
      adoptionStatus: [AdoptionStatus.AVAILABLE, [Validators.required]]
    });

    this.publicationEditForm = this.fb.group({
      title: ['', [Validators.required]],
      description: [''],
      adoptionStatus: [AdoptionStatus.AVAILABLE, [Validators.required]]
    });

    this.loadPets();
    this.loadPublications();
  }

  get titlePlaceholder(): string {
    if (this.selectedPetIds.length === 0) {
      return 'Nombre de la publicacion';
    }

    const fallbackNames = this.pets
      .filter(p => this.selectedPetIds.includes(p.id))
      .map(p => p.name)
      .join(', ');

    return fallbackNames || 'Nombre de la publicacion';
  }

  get selectedPetErrorVisible(): boolean {
    return this.selectedPetIds.length === 0 && this.isSubmitting;
  }

  loadPets(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.errorMessage = 'Inicia sesion para crear una publicacion.';
      return;
    }

    this.petService.getPetsByOwner(userId).subscribe({
      next: (pets) => {
        this.pets = pets;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar tus mascotas.';
      }
    });
  }

  loadPublications(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      return;
    }

    this.publicationService.getPublicationsByAuthor(userId).subscribe({
      next: (publications) => {
        this.publications = publications;
        this.publications.forEach(pub => {
          if (this.publicationImageIndex[pub.id] == null) {
            this.publicationImageIndex[pub.id] = 0;
          }
        });
      },
      error: () => {
        this.publicationActionError = 'No se pudieron cargar tus publicaciones.';
      }
    });
  }

  togglePetSelection(petId: number, checked: boolean): void {
    if (checked) {
      if (!this.selectedPetIds.includes(petId)) {
        this.selectedPetIds = [...this.selectedPetIds, petId];
      }
    } else {
      this.selectedPetIds = this.selectedPetIds.filter(id => id !== petId);
    }

    this.syncPetImagePreviews();
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const files = Array.from(input.files);
    this.selectedFiles = [...this.selectedFiles, ...files];

    files.forEach(file => {
      const reader = new FileReader();
      reader.onload = () => {
        this.previewImages = [
          ...this.previewImages,
          {
            url: reader.result as string,
            name: file.name,
            source: 'upload'
          }
        ];
      };
      reader.readAsDataURL(file);
    });

    input.value = '';
  }

  removeUploadedImage(imageToRemove: PreviewImage): void {
    if (!imageToRemove || imageToRemove.source !== 'upload') {
      return;
    }

    this.previewImages = this.previewImages.filter(img => img !== imageToRemove);
    const fileIndex = this.selectedFiles.findIndex(file => file.name === imageToRemove.name);
    if (fileIndex >= 0) {
      this.selectedFiles.splice(fileIndex, 1);
      this.selectedFiles = [...this.selectedFiles];
    }
  }

  createPublication(): void {
    this.successMessage = '';
    this.errorMessage = '';
    this.isSubmitting = true;

    if (this.postForm.invalid || this.selectedPetIds.length === 0) {
      this.errorMessage = this.selectedPetIds.length === 0
        ? 'Debes asociar al menos una mascota.'
        : 'Revisa los campos del formulario.';
      this.isSubmitting = false;
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.errorMessage = 'Inicia sesion para crear una publicacion.';
      this.isSubmitting = false;
      return;
    }

    const titleRaw = (this.postForm.get('title')?.value as string | null)?.trim() ?? '';
    const descriptionRaw = (this.postForm.get('description')?.value as string | null)?.trim() ?? '';

    const payload = {
      title: titleRaw || null,
      description: descriptionRaw || '',
      adoptionStatus: this.postForm.get('adoptionStatus')?.value as AdoptionStatus,
      petIds: this.selectedPetIds
    };

    const formData = new FormData();
    formData.append('publication', JSON.stringify(payload));
    this.selectedFiles.forEach(file => formData.append('images', file));

    this.publicationService.createPublication(formData, userId).subscribe({
      next: () => {
        this.successMessage = 'Publicacion creada correctamente.';
        this.resetForm();
        this.loadPublications();
      },
      error: () => {
        this.errorMessage = 'No se pudo crear la publicacion. Intenta de nuevo.';
        this.isSubmitting = false;
      }
    });
  }

  private syncPetImagePreviews(): void {
    const petImages: PreviewImage[] = [];

    this.pets
      .filter(pet => this.selectedPetIds.includes(pet.id))
      .forEach(pet => {
        (pet.images ?? []).forEach((img, index) => {
          if (!img.url) {
            return;
          }

          petImages.push({
            url: img.url,
            name: `${pet.name}-${index + 1}`,
            source: 'pet'
          });
        });
      });

    const uploaded = this.previewImages.filter(img => img.source === 'upload');
    this.previewImages = [...petImages, ...uploaded];
  }

  private resetForm(): void {
    this.postForm.reset({
      title: '',
      description: '',
      adoptionStatus: AdoptionStatus.AVAILABLE
    });
    this.selectedPetIds = [];
    this.selectedFiles = [];
    this.previewImages = [];
    this.isSubmitting = false;
  }

  getImageUrl(url?: string): string {
    return url && url.trim().length > 0 ? url : 'assets/placeholder.png';
  }

  handleImageError(event: Event): void {
    const target = event.target as HTMLImageElement;
    target.src = 'assets/placeholder.png';
  }

  getCurrentPublicationImage(publication: PublicationSummary): string {
    const images = publication.images ?? [];
    if (images.length === 0) {
      return 'assets/placeholder.png';
    }

    const currentIndex = this.publicationImageIndex[publication.id] ?? 0;
    const safeIndex = ((currentIndex % images.length) + images.length) % images.length;
    return this.getImageUrl(images[safeIndex]?.url);
  }

  previousPublicationImage(publication: PublicationSummary): void {
    const images = publication.images ?? [];
    if (images.length <= 1) {
      return;
    }

    const current = this.publicationImageIndex[publication.id] ?? 0;
    this.publicationImageIndex[publication.id] = (current - 1 + images.length) % images.length;
  }

  nextPublicationImage(publication: PublicationSummary): void {
    const images = publication.images ?? [];
    if (images.length <= 1) {
      return;
    }

    const current = this.publicationImageIndex[publication.id] ?? 0;
    this.publicationImageIndex[publication.id] = (current + 1) % images.length;
  }

  openPublicationActions(publication: PublicationSummary): void {
    this.selectedPublication = publication;
    this.publicationModalView = 'menu';
    this.publicationActionError = '';
    this.publicationActionMessage = '';
    this.adoptionRequests = [];
    this.isPublicationModalOpen = true;

    this.publicationEditForm.patchValue({
      title: publication.title ?? '',
      description: publication.description ?? '',
      adoptionStatus: publication.adoptionStatus ?? AdoptionStatus.AVAILABLE
    });
  }

  closePublicationModal(): void {
    this.isPublicationModalOpen = false;
    this.publicationModalView = 'menu';
    this.selectedPublication = null;
    this.adoptionRequests = [];
    this.publicationActionError = '';
    this.publicationActionMessage = '';
    this.isLoadingRequests = false;
    this.isSavingPublication = false;
  }

  openEditPublication(): void {
    this.publicationModalView = 'edit';
    this.publicationActionError = '';
    this.publicationActionMessage = '';
  }

  openAdoptionRequests(): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationModalView = 'requests';
    this.isLoadingRequests = true;
    this.publicationActionError = '';
    this.publicationActionMessage = '';

    this.publicationService.getAdoptionRequests(this.selectedPublication.id, userId).subscribe({
      next: (requests) => {
        this.adoptionRequests = requests;
        this.isLoadingRequests = false;
      },
      error: () => {
        this.publicationActionError = 'No se pudieron cargar las solicitudes de adopcion.';
        this.isLoadingRequests = false;
      }
    });
  }

  savePublicationChanges(): void {
    if (!this.selectedPublication || this.publicationEditForm.invalid) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.isSavingPublication = true;
    this.publicationActionError = '';
    this.publicationActionMessage = '';

    const payload = {
      title: String(this.publicationEditForm.get('title')?.value ?? '').trim(),
      description: String(this.publicationEditForm.get('description')?.value ?? '').trim(),
      adoptionStatus: this.publicationEditForm.get('adoptionStatus')?.value as 'AVAILABLE' | 'URGENT' | 'ADOPTED'
    };

    this.publicationService.updatePublication(this.selectedPublication.id, userId, payload).subscribe({
      next: (updatedPublication) => {
        this.selectedPublication = updatedPublication;
        this.publicationActionMessage = 'Publicacion actualizada correctamente.';
        this.isSavingPublication = false;
        this.loadPublications();
      },
      error: () => {
        this.publicationActionError = 'No se pudo actualizar la publicacion.';
        this.isSavingPublication = false;
      }
    });
  }

  updateRequestStatus(requestId: number, status: AdoptionRequestSummary['status']): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationActionError = '';
    this.publicationActionMessage = '';

    this.publicationService
      .updateAdoptionRequestStatus(this.selectedPublication.id, requestId, userId, status)
      .subscribe({
        next: () => {
          this.publicationActionMessage = 'Solicitud actualizada correctamente.';
          this.openAdoptionRequests();
          this.loadPublications();
        },
        error: () => {
          this.publicationActionError = 'No se pudo actualizar la solicitud.';
        }
      });
  }

  deleteRequest(requestId: number): void {
    if (!this.selectedPublication) {
      return;
    }

    const userId = this.getCurrentUserId();
    if (!userId) {
      this.publicationActionError = 'No se encontro el usuario autenticado.';
      return;
    }

    this.publicationActionError = '';
    this.publicationActionMessage = '';

    this.publicationService
      .deleteAdoptionRequest(this.selectedPublication.id, requestId, userId)
      .subscribe({
        next: () => {
          this.publicationActionMessage = 'Solicitud eliminada.';
          this.openAdoptionRequests();
          this.loadPublications();
        },
        error: () => {
          this.publicationActionError = 'No se pudo eliminar la solicitud.';
        }
      });
  }

  formatApplicantName(request: AdoptionRequestSummary): string {
    const firstName = request.applicant?.firstName?.trim() ?? '';
    const lastName = request.applicant?.lastName?.trim() ?? '';
    const fullName = `${firstName} ${lastName}`.trim();
    return fullName || 'Usuario sin nombre';
  }

  getApplicantPhone(request: AdoptionRequestSummary): string {
    const phone = request.applicant?.contactPhone?.trim();
    return phone && phone.length > 0 ? phone : 'Ninguno';
  }

  private getCurrentUserId(): number | null {
    const userJson = localStorage.getItem('user');
    if (!userJson) {
      return null;
    }

    const user = JSON.parse(userJson) as AuthUser;
    return user.id ?? null;
  }

}
