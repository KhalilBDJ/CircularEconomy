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
import java.util.List;
import java.util.Random;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PartStoreAgent extends AgentWindowed {
    List<Part> parts;

    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        this.window.setBackgroundTextColor(Color.LIGHT_GRAY);
        println("hello, I'm just registered as a parts-store");
        println("do you want some special parts ?");
        Random hasard = new Random();
        parts = new ArrayList<>();
        var existingParts = Part.getListParts();
        for (Part p : existingParts) {
            if (hasard.nextBoolean()) {
                parts.add(new Part(p.getName(), p.getType(), p.getStandardPrice() * (1 + Math.random() * .3)));
            }
        }
        if (parts.isEmpty()) parts.add(existingParts.get(hasard.nextInt(existingParts.size())));
        println("here are the parts I sell : ");
        parts.forEach(p -> println("\t" + p));

        AgentServicesTools.register(this, "repair", "partstore");

        // Add a behaviour to listen for CFP messages from UserAgents
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    // Received a CFP message
                    String content = msg.getContent();
                    ACLMessage reply = msg.createReply();

                    Part requestedPart = findPart(content);
                    if (requestedPart != null) {
                        // If the part is available, send a PROPOSE response
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent("Part available: " + requestedPart.getName() + " at " + requestedPart.getStandardPrice() + " EUR");
                    } else {
                        // Otherwise, send a REFUSE response
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Part not available for: " + content);
                    }
                    myAgent.send(reply);
                } else {
                    block();
                }
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
