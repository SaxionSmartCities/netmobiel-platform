package eu.netmobiel.profile.api.resource;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import eu.netmobiel.commons.model.NetMobielUser;
import eu.netmobiel.profile.api.ProfileServiceVersion;
import eu.netmobiel.profile.api.VersionApi;
import eu.netmobiel.profile.api.model.UserRef;
import eu.netmobiel.profile.service.ProfileManager;

@RequestScoped
public class VersionResource extends BasicResource implements VersionApi {

	@Inject
	private ProfileServiceVersion version;

	@Inject
	private ProfileManager profileManager;

	@Override
	public Response getVersion() {
		Response rsp = null;
		try {
			eu.netmobiel.profile.api.model.Version v = new eu.netmobiel.profile.api.model.Version();
			v.setBuildTime(version.getVersionDate());
			v.setBuildVersion(version.getVersionString());
			v.setCommitId(version.getCommitId());
			NetMobielUser nmuser = securityIdentity.getRealUser();
			if (nmuser != null) {
				UserRef ur = new UserRef();
				ur.setId(nmuser.getManagedIdentity());
				ur.setFirstName(nmuser.getGivenName());
				ur.setLastName(nmuser.getFamilyName());
				v.setCaller(ur);
				String effCallerMid = securityIdentity.getEffectivePrincipal().getName();
				if (!effCallerMid.equals(nmuser.getManagedIdentity())) {
					Optional<NetMobielUser> effUser = profileManager.getNetMobielUser(effCallerMid);
					if (effUser.isPresent()) {
						UserRef eur = new UserRef();
						eur.setId(effUser.get().getManagedIdentity());
						eur.setFirstName(effUser.get().getGivenName());
						eur.setLastName(effUser.get().getFamilyName());
						v.setEffectiveCaller(eur);
					}
				}
			}
			rsp = Response.ok(v).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		return rsp;
	}

}
