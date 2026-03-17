import {Site} from './site';

export type TypeMembre = 'GLOBAL' | 'SITE' | 'LIBRE';

export interface Membre {
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeMembre: TypeMembre;
  penaliteActive : boolean;
  finPenalite? : string;
  soldeDu : number;
  site? : Site;
}
