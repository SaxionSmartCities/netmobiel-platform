package eu.netmobiel.planner.model;

public class ModalityUsage {
	public TraverseMode modality;
	public long count;
	
	public ModalityUsage(TraverseMode aModality, long aCount) {
		this.modality = aModality;
		this.count = aCount;
	}

	public TraverseMode getModality() {
		return modality;
	}

	public long getCount() {
		return count;
	}
}

