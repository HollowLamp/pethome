import {
  createRoutesFromElements,
  Route,
  createBrowserRouter,
  Navigate,
} from "react-router";
import UserLayout from "../pages/Layout/UserLayout/UserLayout";
import AdminLayout from "../pages/Layout/AdminLayout/AdminLayout";
import Dashboard from "../pages/Admin/Dashboard/Dashboard";
import RoleManagement from "../pages/Admin/RoleManagement/RoleManagement";

const routes = createRoutesFromElements(
  <Route path="/">
    <Route index element={<UserLayout />}></Route>
    <Route path="admin" element={<AdminLayout />}>
      <Route index element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="role-management" element={<RoleManagement />} />
      {/* 其他admin子路由可以在这里添加 */}
    </Route>
  </Route>
);

const router = createBrowserRouter(routes);

export default router;
