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
import OrgApplication from "../pages/Admin/Org/OrgApplication";
import OrgStaff from "../pages/Admin/Org/OrgStaff";
import OrgApproval from "../pages/Admin/Org/OrgApproval";
import AdoptionApproval from "../pages/Admin/AdoptionApproval/AdoptionApproval";
import AdoptionReview from "../pages/Admin/AdoptionReview/AdoptionReview";
import AdoptionConfirm from "../pages/Admin/AdoptionConfirm/AdoptionConfirm";
import InterviewSchedule from "../pages/Admin/InterviewSchedule/InterviewSchedule";
import PetOverdueReminder from "../pages/Admin/PetOverdueReminder/PetOverdueReminder";
import FeaturedContent from "../pages/Admin/FeaturedContent/FeaturedContent";
import ViolationReview from "../pages/Admin/ViolationReview/ViolationReview";
import UserAppeals from "../pages/Admin/UserAppeals/UserAppeals";
import CommunitySettings from "../pages/Settings/CommunitySettings/CommunitySettings";
import AdoptionProfile from "../pages/Settings/AdoptionProfile/AdoptionProfile";
import PetExplore from "../pages/Pets/Explore/PetExplore";
import PetDetail from "../pages/Pets/Detail/PetDetail";
import Wishlist from "../pages/Wishlist/Wishlist";
import AdoptionRecord from "../pages/AdoptionRecord/AdoptionRecord";
import UserMessages from "../pages/Messages/UserMessages";
import OrgMessages from "../pages/Messages/OrgMessages";
import CommunityExplore from "../pages/Community/Explore/CommunityExplore";
import PostDetail from "../pages/Community/Detail/PostDetail";
import CreatePost from "../pages/Community/Create/CreatePost";
import MyPosts from "../pages/Community/MyPosts/MyPosts";

const routes = createRoutesFromElements(
  <Route path="/">
    <Route element={<UserLayout />}>
      <Route index element={<PetExplore />} />
      <Route path="pets/:petId" element={<PetDetail />} />
      <Route path="wishlist" element={<Wishlist />} />
      <Route path="adoption-record" element={<AdoptionRecord />} />
      <Route path="messages" element={<UserMessages />} />
      <Route path="community">
        <Route index element={<CommunityExplore />} />
        <Route path=":postId" element={<PostDetail />} />
        <Route path="create" element={<CreatePost />} />
        <Route path="my-posts" element={<MyPosts />} />
      </Route>
      <Route path="settings">
        <Route path="community" element={<CommunitySettings />} />
        <Route path="adoption-profile" element={<AdoptionProfile />} />
      </Route>
    </Route>
    <Route path="admin" element={<AdminLayout />}>
      <Route index element={<Navigate to="/admin/dashboard" replace />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="role-management" element={<RoleManagement />} />
      <Route path="pet-management" element={<PetManagement />} />
      <Route path="pet-health" element={<PetHealth />} />
      <Route path="feedback-archive" element={<FeedbackArchive />} />
      <Route path="org-application" element={<OrgApplication />} />
      <Route path="org-staff" element={<OrgStaff />} />
      <Route path="org-approval" element={<OrgApproval />} />
      <Route path="adoption-approval" element={<AdoptionApproval />} />
      <Route path="adoption-review" element={<AdoptionReview />} />
      <Route path="interview-schedule" element={<InterviewSchedule />} />
      <Route path="adoption-confirm" element={<AdoptionConfirm />} />
      <Route path="user-reminders" element={<PetOverdueReminder />} />
      <Route path="featured-content" element={<FeaturedContent />} />
      <Route path="violation-review" element={<ViolationReview />} />
      <Route path="user-appeals" element={<UserAppeals />} />
      <Route path="messages" element={<OrgMessages />} />
      {/* 其他admin子路由可以在这里添加 */}
    </Route>
  </Route>
);

const router = createBrowserRouter(routes);

export default router;
