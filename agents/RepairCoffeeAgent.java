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
import java.io.IOException;
import java.time.LocalDate;
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
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        case "rdv accepte":
                            println("je vérifie ça...");
                            try {
                                checkIfRepairable(message);
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                    }
                }
                else block();
            }
        });
    }

    private void canRepair(ACLMessage message) throws UnreadableException, IOException {
        Product currentProduct = (Product) message.getContentObject();
        for(var speciality: specialities){
            if (currentProduct.getType().equals(speciality)){
                println("oui je peux aider");
                ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                response.addReceiver(message.getSender());
                response.setContent(this.getLocalName());
                response.setConversationId("je peux aider");
                send(response);

                ACLMessage proposition = new ACLMessage(ACLMessage.INFORM);

                proposition.addReceiver(message.getSender());
                Random random = new Random();
                LocalDate date = LocalDate.now().plusDays(random.nextInt(3) + 1);
                println("je vous propose cette date pour vérifier le produit : " + date.toString());
                proposition.setContent(date.toString());
                proposition.setConversationId("proposition de date");
                send(proposition);

                return;
            }
        }

        println("je n'ai pas la specialité");

    }

    private void checkIfRepairable(ACLMessage message) throws UnreadableException {
        Product productToRepair = (Product) message.getContentObject();
        if (productToRepair.getBreakdownLevel() == 4){
            println("Monsieur je suis sincérement navré, mais votre objet est foutu...");
        }
        else {
            println("Bonne nouvelle pour vous monsieur, vous avez la possibilité de le réparer, CEPENDANT, j'espère que vous avec le portfeuille bien fourni");
            var partsStores = AgentServicesTools.searchAgents(this, "repair", "partstore");
            for (var partStore : partsStores){
                ACLMessage priceForPart = new ACLMessage(ACLMessage.REQUEST);
                priceForPart.addReceiver(partStore);
                priceForPart.setConversationId("est-ce que y'a des pieces pour cet objet ?");
                priceForPart.setContent(productToRepair.getDefault().getName());
                send(priceForPart);
            }

        }
    }


}
