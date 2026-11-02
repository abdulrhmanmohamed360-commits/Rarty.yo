import axios from 'axios';
export const apiClient=axios.create({baseURL:import.meta.env.VITE_API_BASE_URL||'/api',timeout:30000,headers:{'Content-Type':'application/json'}});
apiClient.interceptors.response.use(r=>r,e=>Promise.reject(e));
