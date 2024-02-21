package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.ACLMessagesObject.PartAvailableMessage;
import handsOn.circularEconomy.data.Part;
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
import jade.lang.acl.*;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class PartStoreAgent extends AgentWindowed {
    List<Part> parts;

    private boolean isSecondHandSpecialist = false;


    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(), this);
        this.window.setBackgroundTextColor(Color.LIGHT_GRAY);
        AgentServicesTools.register(this, "repair", "partstore");
        Random hasard = new Random();
        this.isSecondHandSpecialist = hasard.nextBoolean();
        println("hello, I'm just registered as a parts-store" + (this.isSecondHandSpecialist ? " specialized in second-hand" : ""));
        println("do you want some special parts ?");
        parts = new ArrayList<>();
        var existingParts = Part.getListParts();
        Collections.shuffle(existingParts);
        for (Part p : existingParts)
            // PartStore can't have part 4 of an object and a maximum of 10 elements
            if (!p.getName().contains("part4") && parts.size() < 10)
                parts.add(new Part(p.getName(), p.getType(), p.getStandardPrice() * (1 + Math.random() * .3)));
        //we need at least one part
        if (parts.isEmpty()) parts.add(existingParts.get(hasard.nextInt(existingParts.size())));
        println("here are the parts I sell : ");
        parts.forEach(p -> println("\t" + p));

        //AgentServicesTools.register(this, "repair", "partstore");


        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (message != null) {
                    switch (message.getConversationId()) {
                        case "est-ce que y'a des pieces pour cet objet ?":
                            try {
                                checkPartAvailability(message);
                            } catch (IOException | UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case "j'achete la piece":
                            println("je vends la pièce suivante:");
                            parts.remove(fromString(message.getContent()));
                            println("voici les pièces que je vends : ");
                            parts.forEach(p -> println("\t" + p));
                        default:
                            break;
                    }
                } else block();
            }
        });
    }

    private void checkPartAvailability(ACLMessage message) throws IOException, UnreadableException {
        for (var part : parts) {
            if (part.getName().equals(message.getContent())) {
                println("je possède la pièce et elle coûte : " + part.getStandardPrice());
                ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                response.addReceiver(message.getSender());
                response.setContent(part.toString());
                response.setConversationId("j'ai la piece");
                send(response);
                return;
            }
        }
        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
        response.addReceiver(message.getSender());
        response.setConversationId("je n'ai pas la piece");
        println("je n'ai pas la pièce");
        send(response);
    }

    private Part findPart(String partName) {
        for (Part part : parts) {
            if (part.getName().equalsIgnoreCase(partName)) {
                return part;
            }
        }
        return null;
    }

    public static Part fromString(String partString) {
        // Regular expression to match the part format
        String regex = "Part\\{ ([^\\-]+\\-part\\d+) \\- ([^\\-]+) \\- ([\\d,]+)€\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(partString);

        if (matcher.find()) {
            String name = matcher.group(1);
            String typeName = matcher.group(2);
            double price = Double.parseDouble(matcher.group(3).replace(",", "."));

            // Find the corresponding ProductType
            ProductType type = ProductType.valueOf(typeName);

            // Create a new Part object with the extracted information
            return new Part(name, type, price);
        } else {
            throw new IllegalArgumentException("String format is incorrect: " + partString);

        }
    }
}
