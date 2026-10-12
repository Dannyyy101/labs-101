import { getProfile } from "./action";
import ProfileView from "./ProfileView";

export default async function Page() {
    return <ProfileView profile={await getProfile()} />
}
