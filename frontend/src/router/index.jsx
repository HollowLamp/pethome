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
import PetManagement from "../pages/Admin/PetManagement/PetManagement";
import PetHealth from "../pages/Admin/PetHealth/PetHealth";
import FeedbackArchive from "../pages/Admin/FeedbackArchive/FeedbackArchive";
import CommunitySettings from "../pages/Settings/CommunitySettings/CommunitySettings";
import PetExplore from "../pages/Pets/Explore/PetExplore";
import PetDetail from "../pages/Pets/Detail/PetDetail";
import Wishlist from "../pages/Wishlist/Wishlist";

const routes = createRoutesFromElements(
  <Route path="/">
    <Route element={<UserLayout />}>
      <Route index element={<PetExplore />} />
      <Route path="pets/:petId" element={<PetDetail />} />
      <Route path="wishlist" element={<Wishlist />} />
    </Route>
    <Route path="admin" element={<AdminLayout />}>
      <Route index element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="role-management" element={<RoleManagement />} />
      <Route path="pet-management" element={<PetManagement />} />
      <Route path="pet-health" element={<PetHealth />} />
      <Route path="feedback-archive" element={<FeedbackArchive />} />
      {/* 其他admin子路由可以在这里添加 */}
    </Route>
    <Route path="settings">
      <Route path="community" element={<CommunitySettings />} />
    </Route>
  </Route>
);

const router = createBrowserRouter(routes);

export default router;
