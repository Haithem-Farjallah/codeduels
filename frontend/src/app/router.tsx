import { LoginPage,RegisterPage } from '@/features/auth';
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute';
import { HomePage } from '@/features/home/pages/HomePage';
import { Layout } from '@/shared/components/Layout';

import { createBrowserRouter, RouterProvider } from 'react-router';

const router = createBrowserRouter([
   
    {
        path:"/login", element:<LoginPage/>
    },
    {
        path:"/register", element:<RegisterPage/>
    },
    {
      element:<Layout/>,
      children:[{
        path:"/",element:<HomePage/>
      }]
    },
   {
    element: <ProtectedRoute />,  
    children: [
      {
        element: <Layout />,            
        children: [
          { path: '/problems', element: <div>problems</div> },
        ],
      },
    ],
  },
])

export const AppRouter=()=><RouterProvider router={router} />