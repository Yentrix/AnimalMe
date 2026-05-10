import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { PetManagementComponent } from './pet-create.component';

describe('PetManagementComponent', () => {
  let component: PetManagementComponent;
  let fixture: ComponentFixture<PetManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PetManagementComponent, HttpClientTestingModule]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PetManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
