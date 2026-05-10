import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { PetService } from '../../services/pet/pet.service';

@Component({
  selector: 'app-pet-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './pet-create.component.html',
  styleUrl: './pet-create.component.css'
})
export class PetManagementComponent implements OnInit {
  viewMode: 'list' | 'kanban' = 'kanban';
  imagePreview: string | null = null;
  pets: any[] = [];
  petForm!: FormGroup;
  selectedFile: File | null = null;
  showModal = false;
  isEditing = false;
  selectedPetId: number | null = null;
  filteredSpecies: any[] = [];
  filteredBreeds: any[] = [];
  allSpecies: any[] = [];
  allBreeds: any[] = [];


  constructor(private fb: FormBuilder, private petService: PetService) { }

  ngOnInit(): void {
    this.initForm();
    this.loadPets();
  }

  initForm() {
    this.petForm = this.fb.group({
      name: ['', [Validators.required]],
      // Los demás son opcionales (sin Validators.required)
      age: [null],
      sex: [''],
      sizeCm: [null],
      description: [''],
      speciesName: [''],
      breedName: [{ value: '', disabled: true }]
    });

    // Escuchar cambios en especie para habilitar raza
    this.petForm.get('speciesName')?.valueChanges.subscribe(value => {
      const breedControl = this.petForm.get('breedName');
      if (value && value.trim().length > 0) {
        breedControl?.enable();
      } else {
        breedControl?.disable();
        breedControl?.setValue('');
      }
    });
  }

  loadPets() {
    // Obtenemos el usuario del localStorage (el que guardamos en el Login)
    const userJson = localStorage.getItem('user');
    if (!userJson) return;

    const user = JSON.parse(userJson);
    const ownerId = user.id;

    this.petService.getPetsByOwner(ownerId).subscribe({
      next: (data) => {
        this.pets = data; // Asignamos el array de mascotas que viene del servidor
        console.log('Mascotas cargadas con éxito:', this.pets);
      },
      error: (err) => {
        console.error('Error al cargar las mascotas:', err);
      }
    });
  }

  editPet(pet: any) {
    this.isEditing = true;
    this.selectedPetId = pet.id;
    this.showModal = true;

    // Rellenamos el formulario con los datos actuales
    this.petForm.patchValue({
      name: pet.name,
      sex: pet.sex,
      sizeCm: pet.sizeCm,
      description: pet.description,
      speciesName: pet.species?.name,
      breedName: pet.breed?.name
    });

    // Si la mascota tiene imagen, mostramos la preview
    if (pet.images && pet.images.length > 0) {
      this.imagePreview = pet.images[0].url;
    }
  }

  selectSpecies(species: any) {
    this.petForm.patchValue({
      speciesName: species.name
    });
    this.filteredSpecies = []; // Limpiamos las sugerencias

    // Opcional: Si quieres cargar las razas de esa especie inmediatamente
    // this.loadBreedsBySpecies(species.id); 
  }

  selectBreed(breed: any) {
    this.petForm.patchValue({
      breedName: breed.name
    });
    this.filteredBreeds = []; // Cerramos la lista
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;

      // Crear la vista previa
      const reader = new FileReader();
      reader.onload = () => {
        this.imagePreview = reader.result as string;
      };
      reader.readAsDataURL(file);
    }
  }

  savePet() {
    if (this.petForm.invalid) return;

    const formData = new FormData();
    const petData = {
      name: this.petForm.get('name')?.value,
      sex: this.petForm.get('sex')?.value,
      sizeCm: this.petForm.get('sizeCm')?.value,
      description: this.petForm.get('description')?.value
    };

    formData.append('pet', JSON.stringify(petData));
    formData.append('speciesName', this.petForm.get('speciesName')?.value);
    formData.append('breedName', this.petForm.get('breedName')?.value);

    if (this.selectedFile) {
      formData.append('image', this.selectedFile);
    }

    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const ownerId = user.id;

    if (this.isEditing && this.selectedPetId) {
      // Actualizar
      this.petService.updatePet(this.selectedPetId, formData).subscribe({
        next: () => this.handleSuccess(),
        error: (err) => console.error('Error al actualizar:', err)
      });
    } else {
      // Crear
      this.petService.createPet(formData, ownerId).subscribe({
        next: () => this.handleSuccess(),
        error: (err) => console.error('Error al crear:', err)
      });
    }
  }

  handleSuccess() {
    this.closeModal();
    this.loadPets();
  }

  handleImageError(event: any) {
    event.target.src = 'assets/placeholder.png';
  }

  closeModal() {
    this.showModal = false;
    this.isEditing = false;
    this.selectedPetId = null;
    this.imagePreview = null;
    this.selectedFile = null;
    this.petForm.reset();
  }

  onSpeciesSearch(event: any) {
    const query = event.target.value;
    if (query.length >= 3) {
      const cleanQuery = this.normalizeQuery(query);
      this.filteredSpecies = this.allSpecies.filter(s =>
        this.normalizeQuery(s.name).includes(cleanQuery)
      );
    } else {
      this.filteredSpecies = [];
    }
  }

  onBreedSearch(event: any) {
    const query = event.target.value;

    if (query.length >= 3) {
      const cleanQuery = this.normalizeQuery(query);

      // Filtramos las razas que coincidan con el texto
      this.filteredBreeds = this.allBreeds.filter(breed => {
        const cleanBreedName = this.normalizeQuery(breed.name);

        // Opcional: Si quieres que solo muestre razas de la especie seleccionada
        const currentSpecies = this.normalizeQuery(this.petForm.get('speciesName')?.value);
        const matchesSpecies = currentSpecies ? this.normalizeQuery(breed.species?.name).includes(currentSpecies) : true;

        return cleanBreedName.includes(cleanQuery) && matchesSpecies;
      });
    } else {
      this.filteredBreeds = [];
    }
  }

  normalizeQuery(text: string): string {
    if (!text) return '';
    return text.normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "") // Quita acentos
      .replace(/[^a-zA-Z0-9 ]/g, "")    // Quita símbolos
      .toLowerCase()
      .trim();
  }
}
