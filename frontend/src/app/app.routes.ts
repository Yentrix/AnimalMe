import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AuthComponent } from './pages/auth/auth.component';
import { HomeComponent } from './pages/home/home.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { PetCreateComponent } from './pages/pet-create/pet-create.component';
import { PostCreateComponent } from './pages/post-create/post-create.component';

export const routes: Routes = [
    {
        path: '',
        component: DashboardComponent,
        children: [
            { path: 'home', component: HomeComponent },
            { path: 'auth', component: AuthComponent },
            { path: 'profile', component: ProfileComponent },
            { path: 'pets', component: PetCreateComponent},
            { path: 'posts', component: PostCreateComponent}
        ]
    }
];
