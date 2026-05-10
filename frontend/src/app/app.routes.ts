import { Routes } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AuthComponent } from './pages/auth/auth.component';
import { HomeComponent } from './pages/home/home.component';
import { ProfileComponent } from './pages/profile/profile.component';
import { PostCreateComponent } from './pages/post-create/post-create.component';
import { PetManagementComponent } from './pages/pet-create/pet-create.component';
import { PostsComponent } from './pages/posts/posts.component';
import { AdminUsersComponent } from './pages/admin-users/admin-users.component';
import { AdminContentComponent } from './pages/admin-content/admin-content.component';
import { AdminNotificationsComponent } from './pages/admin-notifications/admin-notifications.component';

export const routes: Routes = [
    {
        path: '',
        component: DashboardComponent,
        children: [
            { path: 'home', component: HomeComponent },
            { path: 'auth', component: AuthComponent },
            { path: 'profile', component: ProfileComponent },
            { path: 'pets', component: PetManagementComponent},
            { path: 'posts', component: PostsComponent},
            { path: 'posts/create', component: PostCreateComponent},
            { path: 'admin/users', component: AdminUsersComponent},
            { path: 'admin/content', component: AdminContentComponent},
            { path: 'admin/notifications', component: AdminNotificationsComponent},
            { path: '', redirectTo: 'home', pathMatch: 'full' }
        ]
    }
];
