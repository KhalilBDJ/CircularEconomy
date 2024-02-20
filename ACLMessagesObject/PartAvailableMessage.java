package handsOn.circularEconomy.ACLMessagesObject;

import handsOn.circularEconomy.data.Part;
import handsOn.circularEconomy.data.Product;

import java.io.Serializable;

public class PartAvailableMessage implements Serializable {

    private Product productToRepair;
    private Part partToRepair;

    public PartAvailableMessage(Product productToRepair, Part partToRepair) {
        this.productToRepair = productToRepair;
        this.partToRepair = partToRepair;
    }

    public Product getProductToRepair() {
        return productToRepair;
    }

    public void setProductToRepair(Product productToRepair) {
        this.productToRepair = productToRepair;
    }

    public Part getPartToRepair() {
        return partToRepair;
    }

    public void setPartToRepair(Part partToRepair) {
        this.partToRepair = partToRepair;
    }
}
