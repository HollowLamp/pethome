import * as auth from "./auth";
import * as pets from "./pets";
import * as org from "./org";
import * as adoption from "./adoption";
import * as interview from "./interview";
import * as notification from "./notification";

const api = {
  auth,
  pets,
  org,
  adoption,
  interview,
  notification,
};

export default api;
export { auth, pets, org, adoption, interview, notification };
