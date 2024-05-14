import { createApp } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';
import '@/assets/css/main.css';
import App from '@/App.vue';
import List from '@/routes/List.vue';
import Tutorial from '@/routes/Tutorial.vue';
import { createPinia } from 'pinia';

const app = createApp(App);
app.use(createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/tutorial',
            component: Tutorial,
        },
        {
            path: '/:pathMatch(.*)*',
            component: List,
        },
    ],
}));
app.use(createPinia());
app.mount('#app');
