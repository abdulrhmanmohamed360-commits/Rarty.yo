import type {Project,UserProfile} from '../types';
export const user:UserProfile={id:'usr_demo',name:'Alex Morgan',email:'alex@eduforge.app',role:'Educator'};
export const projects:Project[]=[
{id:'p1',title:'The Living World',subject:'Biology',grade:'Grade 9',status:'Writing',progress:68,updatedAt:'12 min ago',pages:42,cover:'linear-gradient(135deg,#123e32,#071b16)'},
{id:'p2',title:'Foundations of Algebra',subject:'Mathematics',grade:'Grade 8',status:'Completed',progress:100,updatedAt:'Yesterday',pages:86,cover:'linear-gradient(135deg,#253b36,#101b19)'},
{id:'p3',title:'Our Changing Planet',subject:'Geography',grade:'Grade 7',status:'Analyzing',progress:24,updatedAt:'2 hours ago',pages:0,cover:'linear-gradient(135deg,#254a42,#13231e)'},
{id:'p4',title:'A Journey Through History',subject:'History',grade:'Grade 10',status:'Draft',progress:0,updatedAt:'Oct 28',pages:0,cover:'linear-gradient(135deg,#4b3825,#1b1711)'}
];
export const activity=[{title:'The Living World',detail:'Writing stage completed',time:'12 min ago',type:'success'},{title:'Our Changing Planet',detail:'Textbook analysis started',time:'2 hours ago',type:'progress'},{title:'Foundations of Algebra',detail:'PDF exported successfully',time:'Yesterday',type:'success'}];
