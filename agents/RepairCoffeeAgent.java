package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.data.Product;
import handsOn.circularEconomy.data.ProductType;
import jade.core.Agent;
import jade.core.AgentServicesTools;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RepairCoffeeAgent extends AgentWindowed {
    List<ProductType> specialities;

    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(),this);
        this.window.setBackgroundTextColor(Color.orange);
        println("hello, do you want coffee ?");
        var hasard = new Random();
        specialities = new ArrayList<>();
        for(ProductType type : ProductType.values())
            if(hasard.nextBoolean()) specialities.add(type);
        //we need at least one speciality
        if(specialities.isEmpty()) specialities.add(ProductType.values()[hasard.nextInt(ProductType.values().length)]);
        println("I have these specialities : ");
        specialities.forEach(p->println("\t"+p));
        //registration to the yellow pages (Directory Facilitator Agent)
        AgentServicesTools.register(this, "repair", "coffee");
        println("I'm just registered as a repair-coffee");

        addBehaviour(new CyclicBehaviour(this){
            public void action(){
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (message != null){
                    switch (message.getConversationId()) {
                        case "puis-je avoir de l'aide ?":
                            try {
                                canRepair(message);
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        default:
                            println(message.getConversationId());
                    }
                }
                else block();
            }
        });
    }

    private void canRepair(ACLMessage message) throws UnreadableException {
        Product currentProduct = (Product) message.getContentObject();
        for(var speciality: specialities){
            if (currentProduct.getType().equals(speciality)){
                println("oui je peux aider");
                return;
            }
        }
        println("je n'ai pas la specialité");

    }


}
