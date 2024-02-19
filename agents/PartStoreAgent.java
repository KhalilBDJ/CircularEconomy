package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.data.Part;
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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PartStoreAgent extends AgentWindowed {
    List<Part> parts;

    private boolean isSecondHandSpecialist = false;


    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(),this);
        this.window.setBackgroundTextColor(Color.LIGHT_GRAY);
        AgentServicesTools.register(this, "repair", "partstore");
        Random hasard = new Random();
        this.isSecondHandSpecialist = hasard.nextBoolean();
        println("hello, I'm just registered as a parts-store" + (this.isSecondHandSpecialist ? " specialized in second-hand" : ""));
        println("do you want some special parts ?");
        parts = new ArrayList<>();
        var existingParts = Part.getListParts();
        Collections.shuffle(existingParts);
        for(Part p : existingParts)
            // PartStore can't have part 4 of an object and a maximum of 10 elements
            if(!p.getName().contains("part4") && parts.size() < 10)
                parts.add(new Part(p.getName(), p.getType(), p.getStandardPrice()*(1+Math.random()*.3)));
        //we need at least one part
        if(parts.isEmpty()) parts.add(existingParts.get(hasard.nextInt(existingParts.size())));
        println("here are the parts I sell : ");
        parts.forEach(p->println("\t"+p));

        //AgentServicesTools.register(this, "repair", "partstore");


        addBehaviour(new CyclicBehaviour(this){
            public void action(){
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (message != null){
                    switch (message.getConversationId()) {
                        case "est-ce que y'a des pieces pour cet objet ?":
                            println("LAISSE MOI DORMIR ZEBI");
                            break;
                        default:
                            println(message.getConversationId());
                    }
                }
                else block();
            }
        });
    }

    private Part findPart(String partName) {
        for (Part part : parts) {
            if (part.getName().equalsIgnoreCase(partName)) {
                return part;
            }
        }
        return null;
    }
}
