export type ProjectStatus='Draft'|'Analyzing'|'Planning'|'Researching'|'Writing'|'Reviewing'|'Designing'|'Completed'|'Failed';
export interface Project {id:string;title:string;subject:string;grade:string;status:ProjectStatus;progress:number;updatedAt:string;pages:number;cover:string}
export interface ApiResult<T>{success:boolean;data:T;message?:string}
export interface UserProfile {id:string;name:string;email:string;role:string;avatar?:string}
