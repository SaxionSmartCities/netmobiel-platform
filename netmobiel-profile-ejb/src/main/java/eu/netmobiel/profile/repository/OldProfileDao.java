package eu.netmobiel.profile.repository;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.exception.BusinessException;
import eu.netmobiel.profile.client.ProfileClient;
import eu.netmobiel.profile.model.Compliment;
import eu.netmobiel.profile.model.Profile;
import eu.netmobiel.profile.model.Review;
import eu.netmobiel.profile.repository.mapping.ComplimentMapper;
import eu.netmobiel.profile.repository.mapping.ProfileMapper;
import eu.netmobiel.profile.repository.mapping.ReviewMapper;


@ApplicationScoped
@Typed(OldProfileDao.class)
public class OldProfileDao {

	@SuppressWarnings("unused")
	@Inject
	private Logger logger;
	
	@Inject
	private ProfileMapper profileMapper;

	@Inject
	private ComplimentMapper complimentMapper;

	@Inject
	private ReviewMapper reviewMapper;

    public OldProfileDao() {
	}

	@Inject
	private ProfileClient profileClient;

	public Profile getProfile(String managedIdentity) throws BusinessException {
		eu.netmobiel.profile.api.model.Profile oldProfile = profileClient.getProfile(managedIdentity);
		Profile p = profileMapper.map(oldProfile);
		p.linkOneToOneChildren();
		p.linkAddresses();
		return p;
	}

	public List<Compliment> getCompliments() throws BusinessException {
		List<eu.netmobiel.profile.api.model.Compliment> oldCompliments = profileClient.getAllCompliments();
		return oldCompliments.stream()
				.map(oc -> complimentMapper.map(oc))
				.collect(Collectors.toList());
	}

	public List<Review> getReviews() throws BusinessException {
		List<eu.netmobiel.profile.api.model.Review> oldReviews = profileClient.getAllReviews();
		return oldReviews.stream()
				.map(or -> reviewMapper.map(or))
				.collect(Collectors.toList());
	}
}
