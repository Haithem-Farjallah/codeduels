export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
}

export interface User{
  id:string,
  username:string;
  avatarUrl:string |null
}