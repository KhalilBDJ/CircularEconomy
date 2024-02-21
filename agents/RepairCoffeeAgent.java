package handsOn.circularEconomy.agents;

import handsOn.circularEconomy.ACLMessagesObject.PartWithStore;
import handsOn.circularEconomy.data.Part;
import handsOn.circularEconomy.data.Product;
import handsOn.circularEconomy.data.ProductType;
import jade.core.AID;
import jade.core.AgentServicesTools;
import jade.core.behaviours.CyclicBehaviour;
import jade.gui.AgentWindowed;
import jade.gui.SimpleWindow4Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepairCoffeeAgent extends AgentWindowed {
    List<ProductType> specialities;
    private AID currentClient;
    private List<PartWithStore> partWithStore;
    private double bestPrice;
    private int partStoreNumber;
    private int check;
    private int noStoreCount;

    @Override
    public void setup() {
        this.window = new SimpleWindow4Agent(getLocalName(),this);
        currentClient = new AID();
        partWithStore = new ArrayList<>();
        bestPrice = 1000000000;
        partStoreNumber = 4;
        check = 0;
        noStoreCount = 0;
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
                                currentClient = message.getSender();
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                            break;
                    }
                }
                else block();
            }
        });

        addBehaviour(new CyclicBehaviour(this){
            public void action(){
                ACLMessage message = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (message != null){
                    switch (message.getConversationId()){
                        case "j'ai la piece":
                            Part part = fromString(message.getContent());
                            check += 1;
                            partWithStore.add(new PartWithStore(message.getSender(), part));
                            if (check == partStoreNumber){
                                getStoreWithBestPriceForPart(partWithStore);
                                partWithStore = new ArrayList<>();
                                check = 0;
                                noStoreCount = 0;

                            }
                            break;
                        case "je n'ai pas la piece":
                            check += 1;
                            noStoreCount += 1;
                            if (check == partStoreNumber){
                                getStoreWithBestPriceForPart(partWithStore);
                                partWithStore = new ArrayList<>();
                                noStoreCount = 0;
                                check =0;
                            }
                            if (noStoreCount == partStoreNumber){
                                println("aucun magasin n'a la pièce");
                                noStoreCount = 0;
                                check = 0;
                            }
                            break;
                    }

                }
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
        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
        response.addReceiver(message.getSender());
        response.setContent(this.getLocalName());
        response.setConversationId("je ne peux pas aider");
        send(response);


        println("je n'ai pas la specialité");

    }

    private void checkIfRepairable(ACLMessage message) throws UnreadableException {
        currentClient = message.getSender();
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


    public PartWithStore getStoreWithBestPriceForPart(List<PartWithStore> partsWithStore) {
        if (partsWithStore == null || partsWithStore.isEmpty()) {
            return null; // Retourne null si la liste est vide ou non initialisée
        }

        PartWithStore bestPricePartWithStore = partsWithStore.get(0); // Initialiser avec le premier élément

        for (PartWithStore partWithStore : partsWithStore) {
            if (partWithStore.getPartToSell().getStandardPrice() < bestPricePartWithStore.getPartToSell().getStandardPrice()) {
                bestPricePartWithStore = partWithStore; // Mettre à jour si un prix inférieur est trouvé
            }
        }

        println("le meilleur store est : " + bestPricePartWithStore.getPartStore().getLocalName() + " et propose la pièce suivante : " + bestPricePartWithStore.getPartToSell().toString());
        return bestPricePartWithStore;
    }

    private void sendCustomerToStore(PartWithStore partWithStore){
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(currentClient);
        message.setConversationId("allez vers ce magasin");

    }


}
