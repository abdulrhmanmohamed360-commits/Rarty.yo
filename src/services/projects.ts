import {projects as seed} from '../mock/data'; import type {Project} from '../types';
const pause=(ms=220)=>new Promise(r=>setTimeout(r,ms));
export const projectService={async list():Promise<Project[]>{await pause();return [...seed]},async create(input:Pick<Project,'title'|'subject'|'grade'>):Promise<Project>{await pause();return {id:crypto.randomUUID(),...input,status:'Draft',progress:0,updatedAt:'Just now',pages:0,cover:'linear-gradient(135deg,#164d3b,#0b1711)'}}};
