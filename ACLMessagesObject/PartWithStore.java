package handsOn.circularEconomy.ACLMessagesObject;

import handsOn.circularEconomy.agents.PartStoreAgent;
import handsOn.circularEconomy.data.Part;
import jade.core.AID;

public class PartWithStore {

    private Part partToSell;
    private AID partStore;

    public PartWithStore(AID PartStore, Part partToSell) {
        this.partToSell = partToSell;
        this.partStore = partStore;
    }

    public Part getPartToSell() {
        return partToSell;
    }

    public AID getPartStore() {
        return partStore;
    }

    public void setPartToSell(Part partToSell) {
        this.partToSell = partToSell;
    }

    public void setPartStore(AID partStore) {
        this.partStore = partStore;
    }
}
