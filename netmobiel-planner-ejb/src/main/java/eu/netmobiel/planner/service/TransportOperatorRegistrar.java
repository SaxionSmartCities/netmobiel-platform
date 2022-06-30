package eu.netmobiel.planner.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;

import eu.netmobiel.commons.util.ExceptionUtil;
import eu.netmobiel.planner.model.TransportOperator;
import eu.netmobiel.planner.model.TraverseMode;
import eu.netmobiel.planner.repository.TransportOperatorApiDao;
import eu.netmobiel.planner.repository.TransportOperatorDao;
import eu.netmobiel.tomp.api.model.AssetClass;
import eu.netmobiel.tomp.api.model.AssetType;

/**
 * Session Bean implementation class TransportOperatorRegistrar
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Lock(LockType.READ)
public class TransportOperatorRegistrar {

	@Inject
    private Logger log;

	@Inject
	private TransportOperatorApiDao operatorApiDao;

	@Inject
	private TransportOperatorDao transportOperatorDao;

	/**
     * Default constructor. 
     */
    public TransportOperatorRegistrar() {
    	traverseMode2Operators = new LinkedHashMap<>();
    	operator2TraverseModes = new LinkedHashMap<>();
    }

	private Map<TraverseMode, Set<TransportOperator>> traverseMode2Operators;
	private Map<TransportOperator, Set<TraverseMode>> operator2TraverseModes;

	private void addSupportedTraverseMode(TransportOperator operator, TraverseMode supportedMode) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Transport Operator '%s' supports %s", operator.getDisplayName(), supportedMode));
		}
		traverseMode2Operators
			.computeIfAbsent(TraverseMode.RIDESHARE, k -> new LinkedHashSet<>())
			.add(operator);
		operator2TraverseModes
			.computeIfAbsent(operator, k -> new LinkedHashSet<>())
			.add(TraverseMode.RIDESHARE);
	}

	private void addOperator(TransportOperator operator) {
		try {
			List<AssetType> assetTypes = operatorApiDao.getAvailableAssets(operator);
			for (AssetType at : assetTypes) {
				if (at.getAssetClass() == AssetClass.CAR && "RIDESHARE".equals(at.getAssetSubClass())) {
					addSupportedTraverseMode(operator, TraverseMode.RIDESHARE);
				} else {
					// Add support for more transport operators
				}
			}
		} catch (Exception ex) {
			log.error(String.join("\n\t", ExceptionUtil.unwindExceptionMessage("Error adding transport operator: " + operator.getDisplayName(), ex)));
		}
	}

	/**
	 * Updates the relation between transport operators and traverse modes.
	 */
	@Lock(LockType.WRITE)
    public void updateRegistry() {
		List<TransportOperator> operators = transportOperatorDao.findAll().stream()
				.filter(to -> to.isEnabled())
				.collect(Collectors.toList());
		traverseMode2Operators.clear();
		operator2TraverseModes.clear();
		for (TransportOperator to : operators) {
			if (to.isEnabled()) {
				addOperator(to);
			}
		}
    }

	public Set<TransportOperator> getOperatorsforTraverseMode(TraverseMode mode) {
		return traverseMode2Operators.containsKey(mode) ? traverseMode2Operators.get(mode) : Collections.emptySet(); 
	}
	
	public boolean hasOperators() {
		return !operator2TraverseModes.isEmpty(); 
	}

}
