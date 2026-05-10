import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AdoptionStatus } from '../../enums/adoption-status';
import { PetService } from '../../services/pet/pet.service';
import {
  PublicationService,
  PublicationUpdatePayload
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
  id?: number;
  url: string;
  name: string;
  source: 'existing' | 'pet' | 'upload';
}

interface AuthUser {
  id?: number;
}

@Component({
  selector: 'app-post-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './post-create.component.html',
  styleUrl: './post-create.component.css'
})
export class PostCreateComponent implements OnInit {
  postForm!: FormGroup;
  pets: PetSummary[] = [];
  availablePets: PetSummary[] = [];
  selectedPetIds: number[] = [];
  selectedFiles: File[] = [];
  previewImages: PreviewImage[] = [];
  removedImageIds: number[] = [];
  manuallyRemovedUrls = new Set<string>();
  associatedPetIds = new Set<number>();
  editablePublicationPetIds = new Set<number>();
  isEditMode = false;
  editingPublicationId: number | null = null;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';
  adoptionStatuses = Object.values(AdoptionStatus);
  readonly statusLabels: Record<AdoptionStatus, string> = {
    [AdoptionStatus.AVAILABLE]: 'Se puede adoptar',
    [AdoptionStatus.URGENT]: 'Urgente de adopcion',
    [AdoptionStatus.ADOPTED]: 'Adoptado'
  };

  constructor(
    private fb: FormBuilder,
    private petService: PetService,
    private publicationService: PublicationService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    this.postForm = this.fb.group({
      title: [''],
      description: [''],
      adoptionStatus: [AdoptionStatus.AVAILABLE, [Validators.required]]
    });

    this.route.queryParamMap.subscribe(params => {
      const publicationIdParam = params.get('publicationId');
      const publicationId = publicationIdParam ? Number(publicationIdParam) : NaN;
      this.isEditMode = Number.isFinite(publicationId) && publicationId > 0;
      this.editingPublicationId = this.isEditMode ? publicationId : null;

      this.loadPetsAndPublications();
    });
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

  get pageTitle(): string {
    return this.isEditMode ? 'Editar Publicacion de Adopcion' : 'Crear Publicacion de Adopcion';
  }

  get pageSubtitle(): string {
    return this.isEditMode
      ? 'Modifica mascotas asociadas, texto e imagenes de la publicacion.'
      : 'Asocia una o varias mascotas y publica su historia.';
  }

  get submitLabel(): string {
    if (this.isSubmitting) {
      return this.isEditMode ? 'Guardando...' : 'Creando...';
    }
    return this.isEditMode ? 'Guardar cambios' : 'Crear publicacion';
  }

  loadPetsAndPublications(): void {
    const userId = this.getCurrentUserId();
    if (!userId) {
      this.errorMessage = 'Inicia sesion para crear una publicacion.';
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    forkJoin({
      pets: this.petService.getPetsByOwner(userId),
      publications: this.publicationService.getPublicationsByAuthor(userId)
    }).subscribe({
      next: ({ pets, publications }) => {
        this.pets = pets;

        const associated = new Set<number>();
        publications.forEach(pub => {
          (pub.pets ?? []).forEach(pet => associated.add(pet.id));
        });
        this.associatedPetIds = associated;

        if (this.isEditMode && this.editingPublicationId != null) {
          const publication = publications.find(pub => pub.id === this.editingPublicationId);
          if (!publication) {
            this.errorMessage = 'No se encontro la publicacion a editar.';
            this.availablePets = [];
            return;
          }

          this.selectedPetIds = (publication.pets ?? []).map(p => p.id);
          this.editablePublicationPetIds = new Set(this.selectedPetIds);
          this.postForm.patchValue({
            title: publication.title ?? '',
            description: publication.description ?? '',
            adoptionStatus: publication.adoptionStatus ?? AdoptionStatus.AVAILABLE
          });

          this.removedImageIds = [];
          this.manuallyRemovedUrls.clear();
          this.previewImages = (publication.images ?? []).map((image, index) => ({
            id: image.id,
            url: image.url,
            name: `actual-${index + 1}`,
            source: 'existing' as const
          }));
        } else {
          this.editablePublicationPetIds = new Set<number>();
          this.resetForm();
        }

        this.rebuildAvailablePets();
        this.syncPetImagePreviews();
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar tus mascotas y publicaciones.';
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
    this.rebuildAvailablePets();
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
    if (!imageToRemove) {
      return;
    }

    if (imageToRemove.source === 'existing' && imageToRemove.id != null) {
      if (!this.removedImageIds.includes(imageToRemove.id)) {
        this.removedImageIds = [...this.removedImageIds, imageToRemove.id];
      }
      this.manuallyRemovedUrls.add(imageToRemove.url);
      this.previewImages = this.previewImages.filter(img => img !== imageToRemove);
      this.syncPetImagePreviews();
      return;
    }

    if (imageToRemove.source !== 'upload') {
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

    if (this.isEditMode && this.editingPublicationId != null) {
      const updatePayload: PublicationUpdatePayload = {
        ...payload,
        removedImageIds: this.removedImageIds
      };

      this.publicationService.updatePublication(this.editingPublicationId, userId, updatePayload, this.selectedFiles).subscribe({
        next: () => {
          this.successMessage = 'Publicacion actualizada correctamente.';
          this.isSubmitting = false;
          setTimeout(() => {
            this.router.navigate(['/posts']);
          }, 500);
        },
        error: () => {
          this.errorMessage = 'No se pudo actualizar la publicacion. Intenta de nuevo.';
          this.isSubmitting = false;
        }
      });
      return;
    }

    this.publicationService.createPublication(formData, userId).subscribe({
      next: () => {
        this.successMessage = 'Publicacion creada correctamente.';
        this.resetForm();
        setTimeout(() => {
          this.router.navigate(['/posts']);
        }, 600);
      },
      error: () => {
        this.errorMessage = 'No se pudo crear la publicacion. Intenta de nuevo.';
        this.isSubmitting = false;
      }
    });
  }

  private syncPetImagePreviews(): void {
    const existingImages = this.previewImages.filter(img => img.source === 'existing');
    const uploadedImages = this.previewImages.filter(img => img.source === 'upload');
    const existingUrls = new Set(existingImages.map(img => img.url));

    const petImages: PreviewImage[] = [];
    this.pets
      .filter(pet => this.selectedPetIds.includes(pet.id))
      .forEach(pet => {
        (pet.images ?? []).forEach((img, index) => {
          if (!img.url || existingUrls.has(img.url) || this.manuallyRemovedUrls.has(img.url)) {
            return;
          }

          petImages.push({
            url: img.url,
            name: `${pet.name}-${index + 1}`,
            source: 'pet'
          });
        });
      });

    this.previewImages = [...existingImages, ...petImages, ...uploadedImages];
  }

  private rebuildAvailablePets(): void {
    const selected = new Set(this.selectedPetIds);
    this.availablePets = this.pets.filter(pet => {
      if (selected.has(pet.id)) {
        return true;
      }
      if (this.isEditMode && this.editablePublicationPetIds.has(pet.id)) {
        return true;
      }
      return !this.associatedPetIds.has(pet.id);
    });
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
    this.removedImageIds = [];
    this.manuallyRemovedUrls.clear();
    this.isSubmitting = false;
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
