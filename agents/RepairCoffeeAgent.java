package handsOn.circularEconomy.agents;

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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RepairCoffeeAgent extends AgentWindowed {
    List<ProductType> specialities;

    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        this.window.setBackgroundTextColor(Color.orange);
        println("Hello, do you want coffee ?");
        Random hasard = new Random();

        specialities = new ArrayList<>();
        for (ProductType type : ProductType.values())
            if (hasard.nextBoolean()) specialities.add(type);
        if (specialities.isEmpty()) specialities.add(ProductType.values()[hasard.nextInt(ProductType.values().length)]);
        println("I have these specialities : ");
        specialities.forEach(p -> println("\t" + p));

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("repair-coffee");
        sd.setName("JADE-repair-coffee");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    ACLMessage reply = msg.createReply();

                    if (canRepair(msg.getContent())) {
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent("Can repair: " + msg.getContent());
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Cannot repair: " + msg.getContent());
                    }
                    myAgent.send(reply);
                } else {
                    block();
                }
            }
        });
    }

    private boolean canRepair(String productTypeString) {
        try {
            ProductType productType = ProductType.valueOf(productTypeString.toUpperCase());
            return specialities.contains(productType);
        } catch (IllegalArgumentException e) {
            println("Received an unknown product type: " + productTypeString);
            return false;
        }
    }
}
