import {
  createRoutesFromElements,
  Route,
  createBrowserRouter,
} from "react-router";
import UserLayout from "../pages/Layout/UserLayout/UserLayout";
import AdminLayout from "../pages/Layout/AdminLayout/AdminLayout";

const routes = createRoutesFromElements(
  <Route path="/">
    <Route index element={<UserLayout />}></Route>
    <Route path="admin" element={<AdminLayout />}></Route>
  </Route>
);

const router = createBrowserRouter(routes);

export default router;
